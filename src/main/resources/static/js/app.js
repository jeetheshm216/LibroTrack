// LibroTrack - Frontend Application Logic

// Auto-detect base API URL (supports both same-origin and cross-origin dev)
const API_BASE = window.location.port === '8080' ? '' : 'http://localhost:8080';

// Global State Cache
const state = {
  books: [],
  students: [],
  issues: [],
  activeIssues: [],
  dashboardStats: null,
  currentPage: 'dashboard'
};

// Document Ready
document.addEventListener('DOMContentLoaded', () => {
  initClock();
  initNavigation();
  initSearchAndFilters();
  navigateTo('dashboard');
});

// Clock
function initClock() {
  const clockEl = document.getElementById('timeString');
  const update = () => {
    const now = new Date();
    clockEl.textContent = now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' });
  };
  update();
  setInterval(update, 1000);
}

// Navigation
function initNavigation() {
  const navLinks = document.querySelectorAll('.nav-link');
  navLinks.forEach(link => {
    link.addEventListener('click', (e) => {
      e.preventDefault();
      const page = link.getAttribute('data-page');
      navigateTo(page);
    });
  });

  const mobileBtn = document.getElementById('mobileMenuBtn');
  const sidebar = document.getElementById('sidebar');
  if (mobileBtn && sidebar) {
    mobileBtn.addEventListener('click', () => {
      sidebar.classList.toggle('mobile-open');
    });
  }
}

function navigateTo(pageId) {
  state.currentPage = pageId;

  // Update Nav links
  document.querySelectorAll('.nav-link').forEach(link => {
    if (link.getAttribute('data-page') === pageId) {
      link.classList.add('active');
    } else {
      link.classList.remove('active');
    }
  });

  // Close mobile sidebar if open
  const sidebar = document.getElementById('sidebar');
  if (sidebar) sidebar.classList.remove('mobile-open');

  // Update Page Views
  document.querySelectorAll('.page-view').forEach(view => {
    view.classList.remove('active');
  });

  const targetView = document.getElementById(`page-${pageId}`);
  if (targetView) targetView.classList.add('active');

  // Header Titles
  const titleEl = document.getElementById('pageTitle');
  const subEl = document.getElementById('pageSubtitle');

  const pageInfo = {
    'dashboard': { title: 'Dashboard', sub: 'Library overview & live statistics' },
    'books': { title: 'Books Catalog', sub: 'Manage library books, inventory, and categories' },
    'students': { title: 'Student Management', sub: 'Register and view student borrowing records' },
    'issue-book': { title: 'Issue Book', sub: 'Assign books to students with automatic 14-day due date' },
    'issued-books': { title: 'Currently Issued Books', sub: 'Active borrowings and return processing' },
    'history': { title: 'Returns / History', sub: 'Complete record of library issues and returns' },
    'fines': { title: 'Fines Management', sub: 'Overdue fine calculations at ₹5 per late day' }
  };

  if (pageInfo[pageId]) {
    titleEl.textContent = pageInfo[pageId].title;
    subEl.textContent = pageInfo[pageId].sub;
  }

  // Load Data for specific page
  switch (pageId) {
    case 'dashboard':
      loadDashboard();
      break;
    case 'books':
      loadBooks();
      break;
    case 'students':
      loadStudents();
      break;
    case 'issue-book':
      prepareIssueBookPage();
      break;
    case 'issued-books':
      loadIssuedBooks();
      break;
    case 'history':
      loadHistory();
      break;
    case 'fines':
      loadFines();
      break;
  }
}

// Search and Filter Listeners
function initSearchAndFilters() {
  const bookSearch = document.getElementById('bookSearchInput');
  if (bookSearch) {
    let timeout = null;
    bookSearch.addEventListener('input', () => {
      clearTimeout(timeout);
      timeout = setTimeout(() => filterBooksTable(), 300);
    });
  }

  const categoryFilter = document.getElementById('bookCategoryFilter');
  if (categoryFilter) {
    categoryFilter.addEventListener('change', () => filterBooksTable());
  }

  const studentSearch = document.getElementById('studentSearchInput');
  if (studentSearch) {
    studentSearch.addEventListener('input', () => filterStudentsTable());
  }

  const historySearch = document.getElementById('historySearchInput');
  if (historySearch) {
    historySearch.addEventListener('input', () => filterHistoryTable());
  }

  const historyStatus = document.getElementById('historyStatusFilter');
  if (historyStatus) {
    historyStatus.addEventListener('change', () => filterHistoryTable());
  }
}

// ---------------- API CALL HELPERS ----------------

async function apiRequest(endpoint, options = {}) {
  try {
    const res = await fetch(`${API_BASE}${endpoint}`, {
      headers: {
        'Content-Type': 'application/json',
        ...(options.headers || {})
      },
      ...options
    });

    const data = await res.json().catch(() => null);

    if (!res.ok) {
      const errorMsg = data && data.message ? data.message : `Request failed with status ${res.status}`;
      throw new Error(errorMsg);
    }
    return data;
  } catch (err) {
    console.error(`API Error on ${endpoint}:`, err);
    throw err;
  }
}

// ---------------- 1. DASHBOARD ----------------

async function loadDashboard() {
  try {
    const stats = await apiRequest('/api/dashboard/stats');
    state.dashboardStats = stats;

    document.getElementById('statTotalBooks').textContent = stats.totalBooks;
    document.getElementById('statTotalStudents').textContent = stats.totalStudents;
    document.getElementById('statAvailableCopies').textContent = stats.availableCopies;
    document.getElementById('statIssuedBooks').textContent = stats.issuedBooks;
    document.getElementById('statOverdueBooks').textContent = stats.overdueBooks;
    document.getElementById('statTotalFines').textContent = `₹${stats.totalFines.toFixed(2)}`;

    // Load active issues for dashboard tables
    const active = await apiRequest('/api/issues/active');
    state.activeIssues = active;

    renderDashboardTables(active);
  } catch (err) {
    showToast('Failed to load dashboard statistics', 'error');
  }
}

function renderDashboardTables(activeIssues) {
  const activeTbody = document.getElementById('dashboardActiveIssuesTable');
  const overdueTbody = document.getElementById('dashboardOverdueTable');

  if (!activeIssues || activeIssues.length === 0) {
    activeTbody.innerHTML = `<tr><td colspan="4" class="text-center empty-state"><div class="empty-state-title">No Active Issues</div><div class="empty-state-desc">All books have been returned.</div></td></tr>`;
    overdueTbody.innerHTML = `<tr><td colspan="4" class="text-center empty-state"><div class="empty-state-title">No Overdue Books</div><div class="empty-state-desc">Great job! All active books are on schedule.</div></td></tr>`;
    return;
  }

  // Active issues (most recent 5)
  const recentActive = activeIssues.slice(0, 5);
  activeTbody.innerHTML = recentActive.map(issue => `
    <tr>
      <td><strong>${escapeHtml(issue.bookTitle)}</strong><br><small style="color: var(--text-muted);">ISBN: ${escapeHtml(issue.bookIsbn)}</small></td>
      <td>${escapeHtml(issue.studentName)}<br><small style="color: var(--text-muted);">${escapeHtml(issue.studentRegisterNumber)}</small></td>
      <td>${issue.dueDate}</td>
      <td>${renderStatusBadge(issue)}</td>
    </tr>
  `).join('');

  // Overdue records
  const overdueList = activeIssues.filter(i => i.overdue);
  if (overdueList.length === 0) {
    overdueTbody.innerHTML = `<tr><td colspan="4" class="text-center empty-state"><div class="empty-state-title">No Overdue Books</div><div class="empty-state-desc">All active loans are within their 14-day limit.</div></td></tr>`;
  } else {
    overdueTbody.innerHTML = overdueList.map(issue => `
      <tr>
        <td><strong>${escapeHtml(issue.bookTitle)}</strong></td>
        <td>${escapeHtml(issue.studentName)} (${escapeHtml(issue.studentRegisterNumber)})</td>
        <td style="color: var(--danger); font-weight: 600;">${issue.dueDate}</td>
        <td><span class="badge badge-danger">${issue.lateDays} days late (Fine: ₹${issue.lateDays * 5})</span></td>
      </tr>
    `).join('');
  }
}

// ---------------- 2. BOOKS ----------------

async function loadBooks() {
  const tbody = document.getElementById('booksTableBody');
  tbody.innerHTML = '<tr><td colspan="7" class="text-center">Loading books...</td></tr>';
  try {
    const books = await apiRequest('/api/books');
    state.books = books;
    populateCategoryFilter(books);
    filterBooksTable();
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="7" class="text-center text-danger">Failed to load books: ${err.message}</td></tr>`;
    showToast(err.message, 'error');
  }
}

function populateCategoryFilter(books) {
  const select = document.getElementById('bookCategoryFilter');
  const currentVal = select.value;
  const categories = [...new Set(books.map(b => b.category).filter(Boolean))].sort();

  select.innerHTML = '<option value="">All Categories</option>' +
    categories.map(c => `<option value="${escapeHtml(c)}">${escapeHtml(c)}</option>`).join('');

  if (categories.includes(currentVal)) {
    select.value = currentVal;
  }
}

function filterBooksTable() {
  const query = (document.getElementById('bookSearchInput').value || '').toLowerCase().trim();
  const category = (document.getElementById('bookCategoryFilter').value || '').trim();

  let filtered = state.books;

  if (category) {
    filtered = filtered.filter(b => b.category === category);
  }

  if (query) {
    filtered = filtered.filter(b => 
      (b.title && b.title.toLowerCase().includes(query)) ||
      (b.author && b.author.toLowerCase().includes(query)) ||
      (b.isbn && b.isbn.toLowerCase().includes(query)) ||
      (b.category && b.category.toLowerCase().includes(query))
    );
  }

  renderBooksTable(filtered);
}

function renderBooksTable(books) {
  const tbody = document.getElementById('booksTableBody');
  if (!books || books.length === 0) {
    tbody.innerHTML = `<tr><td colspan="7" class="text-center empty-state"><div class="empty-state-title">No Books Found</div><div class="empty-state-desc">Try modifying your search or click 'Add Book' to create one.</div></td></tr>`;
    return;
  }

  tbody.innerHTML = books.map(book => {
    const isAvail = book.availableCopies > 0;
    const availBadge = isAvail
      ? `<span class="badge badge-success">${book.availableCopies} available</span>`
      : `<span class="badge badge-danger">Out of Stock</span>`;

    return `
      <tr>
        <td><strong>${escapeHtml(book.title)}</strong></td>
        <td>${escapeHtml(book.author)}</td>
        <td><code>${escapeHtml(book.isbn)}</code></td>
        <td><span class="badge badge-secondary">${escapeHtml(book.category)}</span></td>
        <td><strong>${book.totalCopies}</strong></td>
        <td>${availBadge}</td>
        <td>
          <div style="display: flex; gap: 8px;">
            <button class="btn btn-outline btn-sm" onclick="openEditBookModal(${book.id})" title="Edit Book">
              <i class="fa-solid fa-pen-to-square"></i>
            </button>
            <button class="btn btn-outline btn-sm text-danger" onclick="confirmDeleteBook(${book.id}, '${escapeHtml(book.title.replace(/'/g, "\\'"))}')" title="Delete Book">
              <i class="fa-solid fa-trash"></i>
            </button>
          </div>
        </td>
      </tr>
    `;
  }).join('');
}

function openAddBookModal() {
  document.getElementById('addBookForm').reset();
  openModal('addBookModal');
}

async function handleAddBook(e) {
  e.preventDefault();
  const title = document.getElementById('addBookTitle').value.trim();
  const author = document.getElementById('addBookAuthor').value.trim();
  const isbn = document.getElementById('addBookIsbn').value.trim();
  const category = document.getElementById('addBookCategory').value.trim();
  const totalCopies = parseInt(document.getElementById('addBookTotalCopies').value, 10);

  try {
    await apiRequest('/api/books', {
      method: 'POST',
      body: JSON.stringify({ title, author, isbn, category, totalCopies })
    });
    closeModal('addBookModal');
    showToast('Book added successfully!', 'success');
    loadBooks();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

function openEditBookModal(bookId) {
  const book = state.books.find(b => b.id === bookId);
  if (!book) return;

  document.getElementById('editBookId').value = book.id;
  document.getElementById('editBookTitle').value = book.title;
  document.getElementById('editBookAuthor').value = book.author;
  document.getElementById('editBookIsbn').value = book.isbn;
  document.getElementById('editBookCategory').value = book.category;
  document.getElementById('editBookTotalCopies').value = book.totalCopies;

  const currentlyIssued = book.totalCopies - book.availableCopies;
  document.getElementById('editBookCopiesHelper').textContent = 
    `Currently issued: ${currentlyIssued}. Total copies cannot be reduced below ${currentlyIssued}.`;

  openModal('editBookModal');
}

async function handleEditBook(e) {
  e.preventDefault();
  const id = document.getElementById('editBookId').value;
  const title = document.getElementById('editBookTitle').value.trim();
  const author = document.getElementById('editBookAuthor').value.trim();
  const isbn = document.getElementById('editBookIsbn').value.trim();
  const category = document.getElementById('editBookCategory').value.trim();
  const totalCopies = parseInt(document.getElementById('editBookTotalCopies').value, 10);

  try {
    await apiRequest(`/api/books/${id}`, {
      method: 'PUT',
      body: JSON.stringify({ title, author, isbn, category, totalCopies })
    });
    closeModal('editBookModal');
    showToast('Book updated successfully!', 'success');
    loadBooks();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

function confirmDeleteBook(bookId, title) {
  document.getElementById('deleteConfirmMessage').innerHTML = 
    `Are you sure you want to delete book: <strong>${escapeHtml(title)}</strong>?`;

  const btnConfirm = document.getElementById('btnConfirmDelete');
  btnConfirm.onclick = async () => {
    try {
      await apiRequest(`/api/books/${bookId}`, { method: 'DELETE' });
      closeModal('deleteConfirmModal');
      showToast('Book deleted successfully', 'success');
      loadBooks();
    } catch (err) {
      showToast(err.message, 'error');
    }
  };

  openModal('deleteConfirmModal');
}

// ---------------- 3. STUDENTS ----------------

async function loadStudents() {
  const tbody = document.getElementById('studentsTableBody');
  tbody.innerHTML = '<tr><td colspan="6" class="text-center">Loading students...</td></tr>';
  try {
    const students = await apiRequest('/api/students');
    state.students = students;
    filterStudentsTable();
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="6" class="text-center text-danger">Failed to load students: ${err.message}</td></tr>`;
    showToast(err.message, 'error');
  }
}

function filterStudentsTable() {
  const query = (document.getElementById('studentSearchInput').value || '').toLowerCase().trim();

  let filtered = state.students;
  if (query) {
    filtered = filtered.filter(s => 
      (s.name && s.name.toLowerCase().includes(query)) ||
      (s.registerNumber && s.registerNumber.toLowerCase().includes(query)) ||
      (s.department && s.department.toLowerCase().includes(query)) ||
      (s.email && s.email.toLowerCase().includes(query))
    );
  }

  renderStudentsTable(filtered);
}

function renderStudentsTable(students) {
  const tbody = document.getElementById('studentsTableBody');
  if (!students || students.length === 0) {
    tbody.innerHTML = `<tr><td colspan="6" class="text-center empty-state"><div class="empty-state-title">No Students Found</div><div class="empty-state-desc">Click 'Add Student' to register students.</div></td></tr>`;
    return;
  }

  tbody.innerHTML = students.map(student => `
    <tr>
      <td><strong>${escapeHtml(student.name)}</strong></td>
      <td><code>${escapeHtml(student.registerNumber)}</code></td>
      <td>${escapeHtml(student.email)}</td>
      <td><span class="badge badge-secondary">${escapeHtml(student.department)}</span></td>
      <td>
        <span class="badge ${student.activeIssuesCount > 0 ? 'badge-warning' : 'badge-secondary'}">
          ${student.activeIssuesCount} book${student.activeIssuesCount === 1 ? '' : 's'}
        </span>
      </td>
      <td>
        <button class="btn btn-outline btn-sm" onclick="viewStudentDetails(${student.id})">
          <i class="fa-solid fa-eye"></i>
          <span>View Books</span>
        </button>
      </td>
    </tr>
  `).join('');
}

function openAddStudentModal() {
  document.getElementById('addStudentForm').reset();
  openModal('addStudentModal');
}

async function handleAddStudent(e) {
  e.preventDefault();
  const name = document.getElementById('addStudentName').value.trim();
  const registerNumber = document.getElementById('addStudentReg').value.trim();
  const email = document.getElementById('addStudentEmail').value.trim();
  const department = document.getElementById('addStudentDept').value.trim();

  try {
    await apiRequest('/api/students', {
      method: 'POST',
      body: JSON.stringify({ name, registerNumber, email, department })
    });
    closeModal('addStudentModal');
    showToast('Student registered successfully!', 'success');
    loadStudents();
  } catch (err) {
    showToast(err.message, 'error');
  }
}

async function viewStudentDetails(studentId) {
  try {
    const student = await apiRequest(`/api/students/${studentId}`);
    const issuedBooks = await apiRequest(`/api/students/${studentId}/issued-books`);

    document.getElementById('viewStudentTitle').textContent = `${student.name} - Issued Books`;
    document.getElementById('viewStudentDetails').innerHTML = `
      <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 8px; font-size: 13.5px;">
        <div><strong>Register No:</strong> <code>${escapeHtml(student.registerNumber)}</code></div>
        <div><strong>Department:</strong> ${escapeHtml(student.department)}</div>
        <div><strong>Email:</strong> ${escapeHtml(student.email)}</div>
        <div><strong>Active Borrowings:</strong> <span class="badge badge-warning">${issuedBooks.length}</span></div>
      </div>
    `;

    const tbody = document.getElementById('studentIssuedBooksTable');
    if (!issuedBooks || issuedBooks.length === 0) {
      tbody.innerHTML = `<tr><td colspan="4" class="text-center" style="color: var(--text-muted); padding: 20px;">No books currently issued to this student.</td></tr>`;
    } else {
      tbody.innerHTML = issuedBooks.map(issue => `
        <tr>
          <td><strong>${escapeHtml(issue.bookTitle)}</strong><br><small style="color: var(--text-muted);">ISBN: ${escapeHtml(issue.bookIsbn)}</small></td>
          <td>${issue.issueDate}</td>
          <td>${issue.dueDate}</td>
          <td>${renderStatusBadge(issue)}</td>
        </tr>
      `).join('');
    }

    openModal('viewStudentModal');
  } catch (err) {
    showToast(err.message, 'error');
  }
}

// ---------------- 4. ISSUE BOOK ----------------

async function prepareIssueBookPage() {
  try {
    const [students, books] = await Promise.all([
      apiRequest('/api/students'),
      apiRequest('/api/books')
    ]);

    state.students = students;
    state.books = books;

    // Populate students dropdown
    const studentSelect = document.getElementById('issueStudentSelect');
    studentSelect.innerHTML = '<option value="">-- Choose Student --</option>' +
      students.map(s => `<option value="${s.id}">${escapeHtml(s.name)} (${escapeHtml(s.registerNumber)} - ${escapeHtml(s.department)})</option>`).join('');

    // Populate books dropdown
    const bookSelect = document.getElementById('issueBookSelect');
    bookSelect.innerHTML = '<option value="">-- Choose Book --</option>' +
      books.map(b => {
        const avail = b.availableCopies;
        const label = `${escapeHtml(b.title)} by ${escapeHtml(b.author)} [Available: ${avail}/${b.totalCopies}]`;
        return `<option value="${b.id}" data-available="${avail}">${label}</option>`;
      }).join('');

    // Update Issue info terms preview
    const today = new Date();
    const dueDate = new Date();
    dueDate.setDate(today.getDate() + 14);

    document.getElementById('currentIssueDate').textContent = today.toISOString().split('T')[0];
    document.getElementById('autoDueDate').textContent = `${dueDate.toISOString().split('T')[0]} (14 days from today)`;
    document.getElementById('issueInfoBox').style.display = 'block';

    handleIssueBookSelectChange();
  } catch (err) {
    showToast('Failed to load students and books for issuing', 'error');
  }
}

function handleIssueBookSelectChange() {
  const bookSelect = document.getElementById('issueBookSelect');
  const infoEl = document.getElementById('bookAvailabilityInfo');
  const submitBtn = document.getElementById('btnSubmitIssue');

  const selectedOpt = bookSelect.options[bookSelect.selectedIndex];
  if (!selectedOpt || !selectedOpt.value) {
    infoEl.textContent = '';
    submitBtn.disabled = false;
    return;
  }

  const available = parseInt(selectedOpt.getAttribute('data-available'), 10);
  if (available <= 0) {
    infoEl.innerHTML = `<span style="color: var(--danger); font-weight: 600;"><i class="fa-solid fa-circle-exclamation"></i> Book is currently unavailable. All copies are already issued.</span>`;
    submitBtn.disabled = true;
  } else {
    infoEl.innerHTML = `<span style="color: var(--success); font-weight: 600;"><i class="fa-solid fa-check"></i> ${available} cop${available === 1 ? 'y' : 'ies'} available for issuance.</span>`;
    submitBtn.disabled = false;
  }
}

async function handleIssueBook(e) {
  e.preventDefault();
  const studentId = parseInt(document.getElementById('issueStudentSelect').value, 10);
  const bookId = parseInt(document.getElementById('issueBookSelect').value, 10);

  if (!studentId || !bookId) {
    showToast('Please select both a student and a book.', 'warning');
    return;
  }

  const submitBtn = document.getElementById('btnSubmitIssue');
  submitBtn.disabled = true;
  submitBtn.innerHTML = '<i class="fa-solid fa-spinner fa-spin"></i> Processing...';

  try {
    const result = await apiRequest('/api/issues', {
      method: 'POST',
      body: JSON.stringify({ studentId, bookId })
    });

    showToast(`Book "${result.bookTitle}" issued to ${result.studentName}! Due date: ${result.dueDate}`, 'success');

    // Reset form and redirect to Issued Books view
    document.getElementById('issueBookForm').reset();
    navigateTo('issued-books');
  } catch (err) {
    showToast(err.message, 'error');
  } finally {
    submitBtn.disabled = false;
    submitBtn.innerHTML = '<i class="fa-solid fa-check"></i> Confirm & Issue Book';
  }
}

// ---------------- 5. ISSUED BOOKS ----------------

async function loadIssuedBooks() {
  const tbody = document.getElementById('issuedBooksTableBody');
  tbody.innerHTML = '<tr><td colspan="7" class="text-center">Loading issued books...</td></tr>';
  try {
    const issues = await apiRequest('/api/issues/active');
    state.activeIssues = issues;
    renderIssuedBooksTable(issues);
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="7" class="text-center text-danger">Failed to load issued books: ${err.message}</td></tr>`;
    showToast(err.message, 'error');
  }
}

function renderIssuedBooksTable(issues) {
  const tbody = document.getElementById('issuedBooksTableBody');
  if (!issues || issues.length === 0) {
    tbody.innerHTML = `<tr><td colspan="7" class="text-center empty-state"><div class="empty-state-title">No Currently Issued Books</div><div class="empty-state-desc">All books are in the library. Click 'Issue New Book' to check out a title.</div></td></tr>`;
    return;
  }

  tbody.innerHTML = issues.map(issue => {
    let daysBadge = '';
    if (issue.overdue) {
      daysBadge = `<span class="badge badge-danger"><i class="fa-solid fa-triangle-exclamation"></i> ${issue.lateDays} days overdue</span>`;
    } else {
      daysBadge = `<span class="badge badge-info"><i class="fa-regular fa-clock"></i> ${issue.daysRemaining} days left</span>`;
    }

    return `
      <tr>
        <td><strong>${escapeHtml(issue.bookTitle)}</strong><br><small style="color: var(--text-muted);">${escapeHtml(issue.bookAuthor)} (ISBN: ${escapeHtml(issue.bookIsbn)})</small></td>
        <td><strong>${escapeHtml(issue.studentName)}</strong><br><small style="color: var(--text-muted);">${escapeHtml(issue.studentRegisterNumber)}</small></td>
        <td>${issue.issueDate}</td>
        <td><strong>${issue.dueDate}</strong></td>
        <td>${renderStatusBadge(issue)}</td>
        <td>${daysBadge}</td>
        <td>
          <button class="btn btn-primary btn-sm" onclick="promptReturnBook(${issue.id}, '${escapeHtml(issue.bookTitle.replace(/'/g, "\\'"))}', '${escapeHtml(issue.studentName.replace(/'/g, "\\'"))}', '${issue.dueDate}', ${issue.overdue}, ${issue.lateDays})">
            <i class="fa-solid fa-arrow-rotate-left"></i>
            <span>Return</span>
          </button>
        </td>
      </tr>
    `;
  }).join('');
}

function promptReturnBook(issueId, bookTitle, studentName, dueDate, overdue, lateDays) {
  let fineWarning = '';
  if (overdue) {
    const fine = lateDays * 5;
    fineWarning = `
      <div style="margin-top: 10px; color: var(--danger); font-weight: 600;">
        <i class="fa-solid fa-triangle-exclamation"></i> Overdue by ${lateDays} days. Calculated Fine: ₹${fine} (₹5/day).
      </div>
    `;
  } else {
    fineWarning = `
      <div style="margin-top: 10px; color: var(--success); font-weight: 600;">
        <i class="fa-solid fa-check"></i> Book is returned on time. Fine: ₹0.00.
      </div>
    `;
  }

  document.getElementById('returnConfirmDetails').innerHTML = `
    <div><strong>Book:</strong> ${escapeHtml(bookTitle)}</div>
    <div><strong>Student:</strong> ${escapeHtml(studentName)}</div>
    <div><strong>Due Date:</strong> ${dueDate}</div>
    ${fineWarning}
  `;

  const btnConfirm = document.getElementById('btnConfirmReturn');
  btnConfirm.onclick = async () => {
    try {
      const res = await apiRequest(`/api/issues/${issueId}/return`, { method: 'PUT' });
      closeModal('returnConfirmModal');
      showToast(`Book returned successfully! Fine applied: ₹${res.fineAmount.toFixed(2)}`, 'success');
      loadIssuedBooks();
    } catch (err) {
      showToast(err.message, 'error');
    }
  };

  openModal('returnConfirmModal');
}

// ---------------- 6. RETURNS / HISTORY ----------------

async function loadHistory() {
  const tbody = document.getElementById('historyTableBody');
  tbody.innerHTML = '<tr><td colspan="7" class="text-center">Loading history...</td></tr>';
  try {
    const issues = await apiRequest('/api/issues');
    state.issues = issues;
    filterHistoryTable();
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="7" class="text-center text-danger">Failed to load history: ${err.message}</td></tr>`;
    showToast(err.message, 'error');
  }
}

function filterHistoryTable() {
  const query = (document.getElementById('historySearchInput').value || '').toLowerCase().trim();
  const statusFilter = (document.getElementById('historyStatusFilter').value || '').trim();

  let filtered = state.issues;

  if (statusFilter) {
    filtered = filtered.filter(i => i.status === statusFilter);
  }

  if (query) {
    filtered = filtered.filter(i => 
      (i.bookTitle && i.bookTitle.toLowerCase().includes(query)) ||
      (i.bookAuthor && i.bookAuthor.toLowerCase().includes(query)) ||
      (i.bookIsbn && i.bookIsbn.toLowerCase().includes(query)) ||
      (i.studentName && i.studentName.toLowerCase().includes(query)) ||
      (i.studentRegisterNumber && i.studentRegisterNumber.toLowerCase().includes(query))
    );
  }

  renderHistoryTable(filtered);
}

function renderHistoryTable(issues) {
  const tbody = document.getElementById('historyTableBody');
  if (!issues || issues.length === 0) {
    tbody.innerHTML = `<tr><td colspan="7" class="text-center empty-state"><div class="empty-state-title">No History Found</div><div class="empty-state-desc">No records match the current filters.</div></td></tr>`;
    return;
  }

  tbody.innerHTML = issues.map(issue => `
    <tr>
      <td><strong>${escapeHtml(issue.bookTitle)}</strong><br><small style="color: var(--text-muted);">${escapeHtml(issue.bookAuthor)}</small></td>
      <td>${escapeHtml(issue.studentName)}<br><small style="color: var(--text-muted);">${escapeHtml(issue.studentRegisterNumber)}</small></td>
      <td>${issue.issueDate}</td>
      <td>${issue.dueDate}</td>
      <td>${issue.returnDate ? `<strong>${issue.returnDate}</strong>` : '<span style="color: var(--text-muted);">Not returned</span>'}</td>
      <td>
        ${issue.fineAmount > 0 
          ? `<span class="badge badge-danger">₹${issue.fineAmount.toFixed(2)}</span>` 
          : `<span class="badge badge-secondary">₹0.00</span>`}
      </td>
      <td>${renderStatusBadge(issue)}</td>
    </tr>
  `).join('');
}

// ---------------- 7. FINES ----------------

async function loadFines() {
  const tbody = document.getElementById('finesTableBody');
  tbody.innerHTML = '<tr><td colspan="6" class="text-center">Loading fines...</td></tr>';
  try {
    const [allIssues, stats] = await Promise.all([
      apiRequest('/api/issues'),
      apiRequest('/api/dashboard/stats')
    ]);

    document.getElementById('finesTotalBanner').textContent = `₹${stats.totalFines.toFixed(2)}`;
    document.getElementById('finesActiveOverdueCount').textContent = stats.overdueBooks;

    // Filter issues that either have a recorded fine or are currently overdue
    const finedOrOverdue = allIssues.filter(i => (i.fineAmount && i.fineAmount > 0) || i.overdue);
    const returnedWithFineCount = allIssues.filter(i => i.status === 'RETURNED' && i.fineAmount > 0).length;
    document.getElementById('finesReturnedCount').textContent = returnedWithFineCount;

    if (finedOrOverdue.length === 0) {
      tbody.innerHTML = `<tr><td colspan="6" class="text-center empty-state"><div class="empty-state-title">No Fines Recorded</div><div class="empty-state-desc">All books have been returned on time and no active loans are overdue.</div></td></tr>`;
      return;
    }

    tbody.innerHTML = finedOrOverdue.map(issue => {
      let lateText = `${issue.lateDays} days`;
      let fineBadge = '';

      if (issue.status === 'RETURNED') {
        fineBadge = `<span class="badge badge-danger">₹${issue.fineAmount.toFixed(2)} (Paid)</span>`;
      } else {
        const projectedFine = issue.lateDays * 5;
        fineBadge = `<span class="badge badge-warning">₹${projectedFine.toFixed(2)} (Pending)</span>`;
      }

      return `
        <tr>
          <td><strong>${escapeHtml(issue.studentName)}</strong><br><small style="color: var(--text-muted);">${escapeHtml(issue.studentRegisterNumber)}</small></td>
          <td><strong>${escapeHtml(issue.bookTitle)}</strong><br><small style="color: var(--text-muted);">ISBN: ${escapeHtml(issue.bookIsbn)}</small></td>
          <td>${issue.dueDate}</td>
          <td>${issue.returnDate ? issue.returnDate : '<span class="badge badge-warning">Still Borrowed</span>'}</td>
          <td>${lateText}</td>
          <td>${fineBadge}</td>
        </tr>
      `;
    }).join('');
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="6" class="text-center text-danger">Failed to load fines: ${err.message}</td></tr>`;
    showToast(err.message, 'error');
  }
}


// ---------------- UTILITIES & HELPERS ----------------

function renderStatusBadge(issue) {
  if (issue.status === 'RETURNED') {
    return `<span class="badge badge-success"><i class="fa-solid fa-check"></i> RETURNED</span>`;
  }
  if (issue.overdue) {
    return `<span class="badge badge-danger"><i class="fa-solid fa-triangle-exclamation"></i> OVERDUE</span>`;
  }
  return `<span class="badge badge-warning"><i class="fa-regular fa-clock"></i> ISSUED</span>`;
}

function openModal(id) {
  const modal = document.getElementById(id);
  if (modal) modal.classList.add('active');
}

function closeModal(id) {
  const modal = document.getElementById(id);
  if (modal) modal.classList.remove('active');
}

// Close modals when clicking backdrop
document.addEventListener('click', (e) => {
  if (e.target.classList.contains('modal-overlay')) {
    e.target.classList.remove('active');
  }
});

function showToast(message, type = 'info') {
  const container = document.getElementById('toastContainer');
  if (!container) return;

  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;

  const iconMap = {
    'success': 'fa-solid fa-circle-check',
    'error': 'fa-solid fa-circle-exclamation',
    'warning': 'fa-solid fa-triangle-exclamation',
    'info': 'fa-solid fa-circle-info'
  };

  const titleMap = {
    'success': 'Success',
    'error': 'Error',
    'warning': 'Notice',
    'info': 'Information'
  };

  toast.innerHTML = `
    <i class="${iconMap[type] || iconMap.info} toast-icon"></i>
    <div class="toast-content">
      <div class="toast-title">${titleMap[type] || 'Notice'}</div>
      <div class="toast-msg">${escapeHtml(message)}</div>
    </div>
  `;

  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateX(100%)';
    toast.style.transition = 'all 0.25s ease-out';
    setTimeout(() => toast.remove(), 250);
  }, 4000);
}

function escapeHtml(str) {
  if (!str) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

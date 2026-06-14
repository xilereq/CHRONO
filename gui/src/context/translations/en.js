export const en = {
    // ============= COMMON / GENERAL =============
    common: {
        appName: 'Chrono',
        loading: 'Loading application...',
        logout: 'Logout',
        yes: 'Yes',
        no: 'No',
        cancel: 'Cancel',
        save: 'Save',
        delete: 'Delete',
        edit: 'Edit',
        add: 'Add',
        close: 'Close',
        search: 'Search',
        filter: 'Filter',
        error: 'Error',
        success: 'Success',
        warning: 'Warning',
        info: 'Information',
        back: 'Back',
        next: 'Next',
        previous: 'Previous',
        submit: 'Submit',
        loading_data: 'Loading data...',
        no_data: 'No data',
        settings: 'Settings',
    },

    // ============= SIDEBAR =============
    sidebar: {
        dashboard: 'Dashboard',
        requests: 'Requests',
        reports: 'Reports',
        schedule: 'Schedule',
        userManagement: 'User Management',
        teamSchedule: 'Team Work Hours',
        adminPanel: 'Admin Panel',
        systemLogs: 'System Logs',
        settings: 'Settings',
    },

    // ============= TOPBAR =============
    topbar: {
        notifications: 'Notifications',
        noNewNotifications: 'No new notifications',
        markAsRead: 'Mark as read',
        userLoggedIn: 'Logged in',
        errorFetchingNotifications: 'Error fetching notifications',
        errorParsingNotification: 'Error parsing notification',
        errorSSEConnection: 'SSE connection error',
        markAsReadError: 'Failed to mark as read',
        showPassword: 'Show password',
        hidePassword: 'Hide password',
    },

    // ============= LOGIN PAGE =============
    login: {
        title: 'Login to Chrono',
        subtitle: 'Enter your credentials to continue',
        email: 'Email address',
        password: 'Password',
        loginButton: 'Login',
        loggingIn: 'Logging in...',
        forgotPassword: 'Forgot password',
        emptyFieldsError: 'Please enter email and password.',
        loginError: 'Login error. Check your credentials and try again.',
        emailPlaceholder: 'admin@chrono.pl',
        passwordPlaceholder: '●●●●●●●',
    },

    // ============= FORGOT PASSWORD =============
    forgotPassword: {
        title: 'Recover Password',
        subtitle: 'Enter your email address',
        sendButton: 'Send',
        emailSent: 'Email sent! Check your inbox.',
        invalidEmail: 'Please enter a valid email address.',
        emailNotFound: 'Email address not found.',
    },

    // ============= RESET PASSWORD =============
    resetPassword: {
        title: 'Reset Password',
        newPassword: 'New password',
        confirmPassword: 'Confirm password',
        resetButton: 'Reset Password',
        passwordMismatch: 'Passwords do not match.',
        passwordTooShort: 'Password must be at least 8 characters long.',
        resetSuccess: 'Password changed successfully!',
    },

    // ============= DASHBOARD =============
    dashboard: {
        title: 'Dashboard',
        welcome: 'Welcome',
        quickRequests: 'Quick Requests',
        recentRequests: 'Recent Requests',
        timeTracker: 'Time Tracker',
        statusCharts: 'Status Charts',
        noRecentRequests: 'No recent requests',
        leaveRequest: 'Leave Request',
        workFromHome: 'Work from Home',
        dayOff: 'Day Off',
        scheduleChange: 'Schedule Change',
        quickRequestsTitle: 'QUICK ACTIONS',
        leaveRequestBtn: 'SUBMIT LEAVE REQUEST',
        overtimeTakeBtn: 'TAKE OVERTIME',
        scheduleChangeBtn: 'ADJUST WORK HOURS',
        recentRequestsTitle: 'RECENT REQUESTS',
        mainTitle: 'Time Tracking - Employee Dashboard',
    },

    timeTracker: {
        startWork: 'START WORK',
        endWork: 'END WORK',
        hoStart: 'WFH Start',
        onBreak: 'ON BREAK',
        break: 'BREAK',
        endBreak: 'END BREAK',
        workEndedAlert: 'Work time ended',
        workAlreadyStarted: 'Work is already started in the system.',
        cancelConfirm: 'Are you sure you want to cancel this work entry? This entry will be deleted.',
        entryCancelledAlert: 'Entry cancelled.',
        mistakeCancel: 'Mistake? Cancel entry ('
    },

    statusCharts: {
        yourStatusTitle: 'YOUR STATUS',
        loadingStats: 'Loading statistics...',
        overtimeTitle: 'OVERTIME',
        workTimeTitle: 'WORK TIME (CURRENT MONTH)',
        balance: 'Balance:',
        worked: 'Worked:',
        plan: 'Plan:'
    },



    // ============= REQUESTS PAGE =============
    requests: {
        title: 'Requests',
        myRequests: 'My Requests',
        allRequests: 'All Requests',
        requestType: 'Request Type',
        status: 'Status',
        dateFrom: 'Date From',
        dateTo: 'Date To',
        description: 'Description',
        createRequest: 'Create New Request',
        pending: 'Pending',
        approved: 'Approved',
        rejected: 'Rejected',
        cancelled: 'Cancelled',
        noRequests: 'No requests',
        requestDetails: 'Request Details',
        submitRequest: 'Submit Request',
        cancelRequest: 'Cancel Request',
        viewRequest: 'View Request',
        types: {
            holiday: 'Holiday Leave',
            sick: 'Sick Leave',
            earlyExit: 'Early Exit',
            overtimeEarn: 'Overtime Logging',
            overtimeTake: 'Overtime Take'
        },
        submitDate: 'Submit Date',
        absenceDate: 'Absence Date',
    },

    // ============= APPROVALS PAGE =============
    approvals: {
        title: 'Approvals',
        pendingApprovals: 'Pending Approvals',
        approve: 'Approve',
        reject: 'Reject',
        approvalComment: 'Comment',
        approveSuccess: 'Request approved.',
        rejectSuccess: 'Request rejected.',
    },

    // ============= SCHEDULE PAGE =============
    schedule: {
        title: 'Schedule',
        mySchedule: 'My Schedule',
        workHours: 'Work Hours',
        changeSchedule: 'Change Schedule',
        monday: 'Monday',
        tuesday: 'Tuesday',
        wednesday: 'Wednesday',
        thursday: 'Thursday',
        friday: 'Friday',
        saturday: 'Saturday',
        sunday: 'Sunday',
        startTime: 'Start Time',
        endTime: 'End Time',
        changeSuccessAlert: 'Work hours change request submitted successfully!',
        changeErrorAlert: 'An error occurred while submitting the request.',
        changeWorkHoursTitle: 'Change Work Hours',
        whenDate: 'When (Date)',
        newHoursFrom: 'New Hours (From)',
        newHoursTo: 'New Hours (To)',
        reason: 'Reason',
        reasonPlaceholder: 'Describe why you want to change your hours...'
    },

    // ============= REPORTS PAGE =============
    reports: {
        title: 'Reports',
        generateReport: 'Generate Report',
        myReport: 'My Report',
        teamReport: 'Team Report',
        leaveReport: 'Leave Report',
        worklogReport: 'Work Log Report',
        periodFrom: 'Period From',
        periodTo: 'Period To',
        download: 'Download',
        export: 'Export',
    },

    // ============= USER MANAGEMENT =============
    userManagement: {
        title: 'User Management',
        addUser: 'Add User',
        editUser: 'Edit User',
        deleteUser: 'Delete User',
        firstName: 'First Name',
        lastName: 'Last Name',
        email: 'Email',
        role: 'Role',
        department: 'Department',
        active: 'Active',
        inactive: 'Inactive',
        admin: 'Administrator',
        hr: 'HR',
        manager: 'Manager',
        employee: 'Employee',
        users: 'Users',
        deleteConfirm: 'Are you sure you want to delete this user?',
        deleteSuccess: 'User deleted.',
        userCreated: 'User created.',
        userUpdated: 'User updated.',
    },

    // ============= POLICIES PAGE =============
    policies: {
        title: 'Admin Panel',
        leavePolicy: 'Leave Policy',
        workPolicy: 'Work Policy',
        holidays: 'Public Holidays',
        managers: 'Managers',
        policyName: 'Policy Name',
        description: 'Description',
        createPolicy: 'Create Policy',
        editPolicy: 'Edit Policy',
        deletePolicy: 'Delete Policy',
        setPolicySuccess: 'Policy set.',
        setManagerSuccess: 'Manager assigned.',
        deleteSuccess: 'Policy deleted.',
    },

    // ============= SETTINGS PAGE =============
    settings: {
        title: 'Settings',
        myProfile: 'My Profile',
        changePassword: 'Change Password',
        preferences: 'Preferences',
        language: 'Language',
        theme: 'Theme',
        notifications: 'Notifications',
        currentPassword: 'Current Password',
        newPassword: 'New Password',
        confirmPassword: 'Confirm Password',
        changePasswordButton: 'Change Password',
        passwordChanged: 'Password changed successfully.',
        invalidPassword: 'Current password is incorrect.',
        passwordMismatch: 'Passwords do not match.',
        light: 'Light',
        dark: 'Dark',
    },

    // ============= LOGS PAGE =============
    logs: {
        title: 'System Logs',
        action: 'Action',
        user: 'User',
        timestamp: 'Timestamp',
        details: 'Details',
        filter: 'Filter',
        noLogs: 'No logs.',
    },

    // ============= MANAGER PAGE =============
    manager: {
        title: 'Team Work Hours',
        teamMembers: 'Team Members',
        memberSchedule: 'Member Schedule',
        approveRequest: 'Approve Request',
        rejectRequest: 'Reject Request',
        noTeamMembers: 'No team members.',
    },

    // ============= ERRORS =============
    errors: {
        roleVerificationFailed: 'Role verification error',
        roleVerificationFailedAfterLogin: 'Role verification error after login',
        failedToFetchRole: 'Failed to fetch role',
        networkError: 'Network error. Check your connection.',
        serverError: 'Server error. Try again later.',
        unauthorized: 'Unauthorized.',
        notFound: 'Not found.',
        genericError: 'Something went wrong.',
    },

    // ============= CHANGE PASSWORD MODAL =============
    changePasswordModal: {
        title: 'Change Password',
        subtitle: 'You are required to change your password before using the application.',
        currentPassword: 'Current Password',
        newPassword: 'New Password',
        confirmPassword: 'Confirm New Password',
        changeButton: 'Change Password',
        success: 'Password changed successfully.',
        error: 'Error while changing password.',
        passwordMismatch: 'Passwords do not match.',
        invalidPassword: 'Invalid password.',
    },
};

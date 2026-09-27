Yes. The README can be made **much cleaner** by removing repeated explanations while keeping all important functionality. Here is the shortened full version.

# Expense Intelligence

**Expense Intelligence** is a JavaFX-based Smart Personal Finance Manager developed as a **CSE215 Object-Oriented Programming project**.

It helps users manage income, expenses, budgets, transactions, and financial reports, with additional AI-powered analysis and recommendations through the **Groq API**.

---

## 📌 Project Information

| Category     | Details                              |
| ------------ | ------------------------------------ |
| Course       | CSE215 — Object-Oriented Programming |
| Project Type | Java Desktop Application             |
| Language     | Java                                 |
| GUI          | JavaFX                               |
| Build Tool   | Maven                                |
| AI           | Groq API                             |
| Storage      | Local Text Files                     |
| Currency     | Bangladeshi Taka (৳)                 |

---

# ✨ Features

## 🔐 Authentication

* User registration and login
* Input validation
* SHA-256 password hashing
* Session management
* User-specific financial data

## 📊 Dashboard

* Monthly income, expenses, balance, and savings rate
* Spending LineChart and expense PieChart
* Recent transactions
* Budget progress and alerts
* Dynamic user greeting

## 💰 Income Management

Full CRUD, search, and filtering for income records.

**Categories:** Salary, Freelance, Business, Investment, Gift, Other

## 💸 Expense Management

Full CRUD, search, and filtering for expense records.

**Categories:** Food, Bills, Transport, Shopping, Entertainment, Other

## 📋 Transaction History

* Unified income and expense records
* Search and filtering by date, type, category, description, and amount
* Uses `List<Transaction>` for polymorphic transaction handling

## 🎯 Budget Management

* Category-based budgets
* Create, edit, and delete budgets
* Spending progress tracking
* 80% warning and over-budget alerts

## 📈 Financial Reports

* Custom date-range reports
* Income, expense, and balance summaries
* Financial comparisons
* Multi-month analysis and charts
* Local report storage

---

# 🤖 AI Intelligence

The application integrates the **Groq API** for financial analysis and recommendations.

### 🧠 Spending Analysis

Analyzes recorded financial data including:

* Current and previous month spending
* Top spending categories
* Savings rate
* Budget usage
* Historical spending patterns

Provides concise financial insights and recommendations.

### 💬 Financial Advisor

Users can ask natural-language questions about their finances.

Examples:

```text
How much did I spend this month?
Which category costs me the most?
How much did I save compared to last month?
```

The advisor uses current-month data, previous-month comparisons, and available historical records.

### 🎯 Budget Recommendation

Users provide their monthly income and target savings.

**Spend Ceiling = Monthly Income − Target Savings**

Recommendations are generated using transaction history, spending patterns, existing budgets, and active categories.

The system ensures that:

* Total budget equals the spending ceiling
* Only active categories are used
* Values use practical ৳100 increments
* Recommendations can be directly applied to budgets

---

# 💱 Currency Support

All financial values use **Bangladeshi Taka (৳)**.

The AI response formatter converts common currency formats such as `$`, `Rs`, `INR`, `Tk`, `BDT`, `₹`, and `₨` into `৳`.

---

# 🧩 OOP Concepts

The project demonstrates the following Java OOP concepts:

### Abstraction

Abstract `Transaction` class with methods such as:

```java
getType()
calculateImpact()
```

### Inheritance

```text
Transaction
├── Income
└── Expense
```

### Polymorphism

```java
List<Transaction>
```

allows `Income` and `Expense` objects to be handled through the common `Transaction` type.

### Encapsulation

Private fields with constructors, getters, and setters are used in classes such as:

`User`, `Transaction`, `Income`, `Expense`, `Budget`, and `FinancialReport`.

### Interfaces

* `Reportable` → `FinancialReport`
* `Analyzable` → `SpendingInsight`

### Separation of Concerns

GUI, services, models, utilities, API communication, and storage are separated into dedicated components.

---

# 🏗️ Project Architecture

Expense Intelligence follows a layered architecture that separates the user interface, business logic, data handling, and AI functionality.

## Main Application Flow

```text
JavaFX GUI
    │
    ▼
Service Layer
    │
    ▼
FileManager
    │
    ▼
Local File Storage
```

## AI Processing Flow

```text
JavaFX AI Interface
        │
        ▼
    AIService
        │
        ▼
    GroqClient
        │
        ▼
   Groq AI API
        │
        ▼
AIResponseFormatter
        │
        ▼
JavaFX AI Interface
```

## Architecture Layers

* **GUI Layer** — JavaFX interface and user interaction
* **Service Layer** — Business logic and financial operations
* **Model Layer** — Users, transactions, income, expenses, budgets, and reports
* **Utility Layer** — Validation, file management, sessions, dates, and AI formatting
* **API Layer** — Groq API communication
* **Storage Layer** — Local text-file storage

---

# 📁 Project Structure

```text
ExpenseIntelligence/
│
├── src/
│   └── main/
│       ├── java/
│       │   └── Program/
│       │       ├── api/
│       │       │   └── GroqClient.java
│       │       ├── enums/
│       │       │   ├── ExpenseCategory.java
│       │       │   ├── IncomeCategory.java
│       │       │   └── UserRole.java
│       │       ├── interfaces/
│       │       │   ├── Analyzable.java
│       │       │   └── Reportable.java
│       │       ├── model/
│       │       │   ├── Budget.java
│       │       │   ├── Expense.java
│       │       │   ├── FinancialReport.java
│       │       │   ├── Income.java
│       │       │   ├── SpendingInsight.java
│       │       │   ├── Transaction.java
│       │       │   └── User.java
│       │       ├── service/
│       │       │   ├── AIService.java
│       │       │   ├── AuthService.java
│       │       │   ├── BudgetService.java
│       │       │   ├── DataService.java
│       │       │   ├── ExpenseService.java
│       │       │   ├── IncomeService.java
│       │       │   ├── ReportService.java
│       │       │   └── TransactionService.java
│       │       ├── utils/
│       │       │   ├── AIResponseFormatter.java
│       │       │   ├── DateUtils.java
│       │       │   ├── FileManager.java
│       │       │   ├── SessionManager.java
│       │       │   └── Validator.java
│       │       ├── gui/
│       │       │   ├── components/
│       │       │   ├── styles/
│       │       │   └── UI classes
│       │       └── Main.java
│       │
│       └── resources/
│
├── File/
│   ├── users.txt
│   ├── incomes.txt
│   ├── expenses.txt
│   ├── budgets.txt
│   └── reports/
│       └── monthly_reports.txt
│
├── pom.xml
├── .gitignore
└── README.md
```

---

# ⚙️ Requirements

* **JDK 21+**
* **Maven 3.9+**
* **Git**
* IntelliJ IDEA, VS Code, or another Java IDE
* Internet connection for AI features
* Groq API key for AI functionality

---

# 🚀 How to Run

### 1. Clone the Repository

```bash
git clone https://github.com/your-username/expense-intelligence.git
cd expense-intelligence
```

### 2. Set the Groq API Key

**PowerShell:**

```powershell
$env:GROQ_API_KEY = "your-groq-api-key"
```

For a permanent environment variable:

```powershell
[Environment]::SetEnvironmentVariable(
    "GROQ_API_KEY",
    "your-groq-api-key",
    "User"
)
```

### 3. Run the Application

```bash
mvn clean javafx:run
```

> **Note:** Core expense-management features work without the API key. The key is required for AI features.

---

# 💾 Data Storage

The application uses local text files instead of a database:

```text
users.txt
incomes.txt
expenses.txt
budgets.txt
monthly_reports.txt
```

This keeps the project lightweight and easy to run in an academic environment.

---

# 🔒 Security

The application includes:

* Password hashing
* User authentication
* Session management
* Input validation
* User-specific data handling

**Never commit API keys or personal financial data to GitHub.**

---

# 🚫 `.gitignore`

```gitignore
target/
.idea/
.vscode/
*.iml

.env

File/users.txt
File/incomes.txt
File/expenses.txt
File/budgets.txt
File/reports/
```

---

# 🔮 Future Improvements

* Database integration
* Password reset
* Recurring transactions
* Financial notifications
* PDF reports
* Advanced charts
* Mobile application
* Multi-currency support
* Cloud synchronization
* AI personalization
* Improved authentication
* Data export/import

---

# 🎓 Academic Purpose

Developed as a **CSE215 Object-Oriented Programming project** to demonstrate practical Java OOP concepts through a real-world personal finance management application.

### Technologies

```text
Java
+
JavaFX
+
OOP
+
File Handling
+
Maven
+
REST API
+
AI Integration
```

---

# 📄 License

This project was developed for **academic purposes as part of the CSE215 Object-Oriented Programming course**.

© 2026 Expense Intelligence

<div align="center">
  <img src="frontend/public/logo_transparent.png" alt="Klex Logo" width="180" />
  <h1>Klex</h1>
  <p><strong>A Modern, Decentralized Enterprise Reporting Server</strong></p>
  <p>Run, manage, and schedule <a href="https://community.klex server community edition.com/">Klex</a> using an elegant web interface, backed by powerful decentralized microservices.</p>
</div>

<br />

## ✨ What is Klex?

Klex is a full-stack open-source platform that brings modern web features and robust service-oriented architecture to the KlexReports ecosystem. Instead of a monolithic server, Klex orchestrates reporting through discrete, highly-scalable components.

It allows organizations to:
- Connect directly to **GitHub repositories** to sync report templates (`.jrxml` files)
- Process reports in real-time or via **cron-based scheduling**
- Connect dynamically to multiple **Data Adapters** (JDBC, CSV, JSON)
- Leverage a beautiful, responsive, **React-based UI** for managing permissions and logs

## 🏗️ The Architecture

Klex uses a decoupled, hybrid-stack approach to maximize performance and maintainability:

1. **Klex UI (React + Vite)**: A premium, glassmorphism-inspired web interface. 
2. **Management Backend (Django)**: Handles Auth, RBAC, GitHub OAuth, and orchestrates the Java services.
3. **Reporting Service (Java Spring Boot)**: The heavy-lifter that compiles KlexReports and executes them against your dynamic Data Adapters.
4. **Scheduler Service (Java Spring Boot)**: Uses Quartz Scheduler to manage recurring report deliveries via Email, S3, or Google Drive.

👉 **[Read the Full Architecture Guide](docs/ARCHITECTURE.md)**

---

## 📚 Documentation

Full documentation for Klex is maintained in a separate repository:

📖 **[Klex Documentation](../docs)** — Getting started, guides, API reference, deployment, and more.

| Section | Description |
|---|---|
| [Getting Started](../docs/getting-started/) | Installation, prerequisites, and quick-start guide |
| [Architecture](../docs/architecture/) | System design, component diagrams, service descriptions |
| [Guides](../docs/guides/) | GitHub integration, data adapters, scheduling, report compilation |
| [Configuration](../docs/configuration/) | Environment variables, database setup, OAuth setup |
| [Deployment](../docs/deployment/) | Docker Compose, production hosting, cloud deployment |
| [API Reference](../docs/api-reference/) | REST API endpoints documentation |
| [Contributing](../docs/contributing/) | Developer setup, code structure, PR guidelines |
| [FAQ](../docs/faq/) | Common issues and troubleshooting |

---

## 🚀 Quick Start

### Prerequisites
- **Python 3.11+** with [uv](https://docs.astral.sh/uv/) package manager
- **Node.js 18+** with npm
- **Java 8** (OpenJDK 1.8.0_482) with **Maven 3.8.7** (`mvn`)
- **PostgreSQL 14+**

### 1. Database Setup
```bash
# Create the primary Django database
createdb klex

# Create the Quartz scheduler database
createdb quartz
```

### 2. Environment Configuration
Copy the environment template and fill in your details:
```bash
cp .env.example .env
```

**Required in `.env`:**
- `DATABASE_URL` (e.g. `postgres://localhost/klex`)
- `DJANGO_SECRET_KEY`
- `GITHUB_CLIENT_ID` & `GITHUB_CLIENT_SECRET` (For OAuth repo syncing)

### 3. Start All Services
Klex includes a unified startup script that launches the frontend, backend, and both Java microservices simultaneously:

```bash
chmod +x start.sh
./start.sh
```

**What this does:**
- Runs Django migrations and starts backend on `:8000`
- Installs `npm` dependencies and starts React UI on `:5173`
- Starts the `reporting-service` on `:8081`
- Starts the `scheduler` service on `:8082`

---

## 📖 Key Features & Workflows

### 🔗 Connecting GitHub Repositories
Reports are no longer uploaded manually. Connect Klex to your GitHub account via OAuth, select a repository containing `.jrxml` files, and Klex will sync them instantly. Whenever you push changes to your `main` branch, Klex always fetches the latest version before compiling.

### 🗄️ Dynamic Data Adapters
Manage connections to your external databases—such as PostgreSQL, MySQL, and MS SQL Server—directly from the Klex UI. The Reporting Service dynamically injects these credentials at runtime to pull live data into your reports.

### ⏱️ Advanced Scheduling
Use the Schedule Modal in the Report Viewer to configure complex Quartz cron triggers. Choose your desired output formats (PDF/XML/CSV) and configure delivery methods like direct Email attachments or Google Drive uploads.

### 🎨 Premium Design System
We've overhauled standard enterprise software aesthetics. Klex features a bespoke indigo and amber color palette, seamless dark mode support, glassmorphism overlays, and fluid micro-animations—making data management visually engaging.

---

## 📁 Repository Structure

```text
klex/
├── backend/                 # Django Management API (Auth, Repos, Audit)
├── frontend/                # React / Vite / TailwindCSS Application
├── services/
│   ├── reporting-service/   # Java / KlexReports compilation engine
│   └── scheduler/           # Java / Quartz scheduling engine
├── docs/                    # Architecture & Component documentation
├── start.sh                 # Unified development boot script
└── .env.example             # Environment configuration template
```

## 🤝 Contributing

We welcome contributions! Please see our **[Contributing Guide](../docs/contributing/)** for details on how to get started.

- Review the [documentation](../docs) and submit pull requests for improvements
- [Report bugs or request features](../../issues) via GitHub Issues
- Read the [developer setup guide](../docs/contributing/development-setup.md) to build from source

## 🔒 Security

- **JSON Web Tokens (JWT)**: Used for robust session management.
- **RBAC**: Fine-grained permissions per report folder.
- **Credential Storage**: Database connection details are securely stored. OAuth tokens are managed confidentially.
- **Auditing**: Built-in Audit Logs (visible in the Admin UI) track all critical actions like repo syncing and data adapter creation.

## 📄 License
This project is licensed under the [Apache License 2.0](LICENSE) and copyright (c) 2026 Le Quirks.

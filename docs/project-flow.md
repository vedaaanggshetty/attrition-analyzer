# Attrition Analyzer — Project Flows

This document visualizes the Attrition Analyzer system using diagrams rendered directly from Markdown (Mermaid), so the flows stay readable straight from the GitHub repository. It covers the system architecture, the guest → HR user journey, authentication, employee search, attrition analysis, notifications, service discovery, and database ownership.

These diagrams are derived from the reference designs in `docs/diagrams/` (numbered PNGs `01`–`09`), which remain the authoritative source. This file is a text-based, versionable companion to those images — it does not introduce new functionality, services, or architecture beyond what the reference diagrams show.

**Reading the diagrams:** solid arrows represent an actual runtime request/data flow; dotted arrows represent service registration/discovery with Eureka, which is never itself a hop in a request path; cylinder shapes are databases; the parallelogram shape is an external system (the Survey API).

---

## 1. System Overview

*Based on `03_System_Architecture.png`.*

```mermaid
flowchart TD
    Guest([Guest / HR User])
    FE[React Frontend]
    GW[API Gateway]

    subgraph Services["Microservices"]
        AUTH[Authentication Service]
        PROFILE[User Profile Service]
        EMP[Employee Service]
        NOTIF[Notification Service]
    end

    EUREKA{{Eureka Discovery Service}}
    SURVEY[/External Survey API/]
    KAFKA[[Kafka]]

    AUTHDB[(Authentication DB)]
    PROFILEDB[(User Profile DB)]
    NOTIFDB[(Notification DB)]

    Guest --> FE
    FE --> GW
    GW -->|routes request| AUTH
    GW -->|routes request| PROFILE
    GW -->|routes request| EMP
    GW -->|routes request| NOTIF

    GW -.->|discovers services| EUREKA
    AUTH -.->|registers| EUREKA
    PROFILE -.->|registers| EUREKA
    EMP -.->|registers| EUREKA
    NOTIF -.->|registers| EUREKA

    EMP -->|queries| SURVEY
    NOTIF -->|publishes event| KAFKA

    AUTH --> AUTHDB
    PROFILE --> PROFILEDB
    NOTIF --> NOTIFDB

    classDef actor fill:#f5d0fe,stroke:#701a75,color:#3b0764
    classDef frontend fill:#99f6e4,stroke:#0f766e,color:#134e4a
    classDef gateway fill:#fed7aa,stroke:#c2410c,color:#7c2d12
    classDef service fill:#bae6fd,stroke:#0369a1,color:#0c4a6e
    classDef eureka fill:#e5e7eb,stroke:#4b5563,stroke-dasharray: 4 3,color:#1f2937
    classDef external fill:#bbf7d0,stroke:#15803d,color:#14532d
    classDef kafka fill:#ddd6fe,stroke:#6d28d9,color:#3b0764
    classDef db fill:#dcfce7,stroke:#166534,color:#14532d

    class Guest actor
    class FE frontend
    class GW gateway
    class AUTH,PROFILE,EMP,NOTIF service
    class EUREKA eureka
    class SURVEY external
    class KAFKA kafka
    class AUTHDB,PROFILEDB,NOTIFDB db
```

Eureka is drawn off to the side and connected only with dotted "registers / discovers" edges — it is a registry that services and the gateway consult, not a stop on the request path itself.

---

## 2. Guest Registration

*Based on `02_RegistrationFlow.png`.*

Only two user types exist in the system: **Guest** and **HR**. A Guest who completes registration becomes an HR user.

```mermaid
flowchart TD
    Guest([Guest User])
    Open[Open Application]
    Limited[View Limited Attrition Information]
    ChooseReg[Choose Register]
    FE[React Frontend]
    GW[API Gateway]
    AUTH[Authentication Service]
    Enter[Enter Registration Details]
    Valid{Details Valid?}
    Error[Show Error]
    Create[Create HR User Account]
    HR([HR User])
    Login[Login]
    Full[Access Full HR Features]

    Guest --> Open --> Limited --> ChooseReg --> FE
    FE -->|registration request| GW
    GW -->|forward| AUTH
    AUTH --> Enter --> Valid
    Valid -->|No| Error --> Enter
    Valid -->|Yes, successful registration| Create --> HR
    HR --> Login --> Full

    classDef actor fill:#f5d0fe,stroke:#701a75,color:#3b0764
    classDef frontend fill:#99f6e4,stroke:#0f766e,color:#134e4a
    classDef gateway fill:#fed7aa,stroke:#c2410c,color:#7c2d12
    classDef service fill:#bae6fd,stroke:#0369a1,color:#0c4a6e
    classDef decision fill:#fef08a,stroke:#a16207,color:#713f12
    classDef error fill:#fecaca,stroke:#b91c1c,color:#7f1d1d
    classDef success fill:#bbf7d0,stroke:#15803d,color:#14532d

    class Guest,HR actor
    class FE frontend
    class GW gateway
    class AUTH service
    class Valid decision
    class Error error
    class Create,Login,Full success
```

---

## 3. Login Flow

*Based on `06_LoginFlow.png`.*

```mermaid
sequenceDiagram
    actor HR as HR User
    participant FE as React Frontend
    participant GW as API Gateway
    participant AUTH as Authentication Service
    participant DB as Authentication DB

    HR->>FE: Enter username + password
    FE->>GW: POST /login
    GW->>AUTH: Login request
    AUTH->>DB: Validate credentials
    DB-->>AUTH: Valid / Invalid

    alt Valid credentials
        AUTH->>AUTH: Generate JWT
        AUTH-->>GW: JWT
        GW-->>FE: JWT
        FE-->>HR: Logged in
    else Invalid credentials
        AUTH-->>GW: Authentication error
        GW-->>FE: Error
        FE-->>HR: Show login error
    end

    note over FE,AUTH: Session expires after configured inactivity period
```

---

## 4. Employee Search

*Based on `04_SearchEmployeeFlow.png`.*

Search stays a lightweight pass-through: the frontend's search request is forwarded through the existing Employee Service straight to the Survey API — there is no dedicated search microservice or additional infrastructure.

```mermaid
sequenceDiagram
    actor HR as HR User
    participant FE as React Frontend
    participant GW as API Gateway
    participant EMP as Employee Service
    participant SURVEY as Survey API

    HR->>FE: Enter search property and value
    FE->>GW: Search request
    GW->>EMP: Forward request
    EMP->>SURVEY: GET /survey?propertyname=value
    SURVEY-->>EMP: Filtered employee data
    EMP-->>GW: Search results
    GW-->>FE: Matching employees
    FE-->>HR: Display results
```

---

## 5. Attrition Analysis

*Based on `05_AttritionAnalysisFlow.png`.*

```mermaid
flowchart TD
    HR([HR User])
    Select[Select Analysis]
    Dept[Department]
    Role[Job Role]
    Comp[Compensation]
    Demo[Demographics]
    WLB[Work-Life Balance]
    Career[Career Progression]
    EMP[Employee Service]
    SURVEY[/Survey API/]
    Data[Employee Data]
    Analysis[Attrition Analysis]
    Results[Analysis Results]
    FE[React Frontend]

    HR --> Select
    Select --> Dept & Role & Comp & Demo & WLB & Career
    Dept --> EMP
    Role --> EMP
    Comp --> EMP
    Demo --> EMP
    WLB --> EMP
    Career --> EMP
    EMP --> SURVEY --> Data --> Analysis --> Results --> FE --> HR

    classDef actor fill:#f5d0fe,stroke:#701a75,color:#3b0764
    classDef criteria fill:#fde68a,stroke:#b45309,color:#78350f
    classDef service fill:#bae6fd,stroke:#0369a1,color:#0c4a6e
    classDef external fill:#bbf7d0,stroke:#15803d,color:#14532d
    classDef frontend fill:#99f6e4,stroke:#0f766e,color:#134e4a

    class HR actor
    class Dept,Role,Comp,Demo,WLB,Career criteria
    class EMP,Analysis service
    class SURVEY external
    class FE frontend
```

No analysis categories beyond the six shown above (Department, Job Role, Compensation, Demographics, Work-Life Balance, Career Progression) are implied.

---

## 6. Notification Flow

*Based on `07_CreateNotificationFlow.png`.*

```mermaid
sequenceDiagram
    actor HR as HR User
    participant FE as React Frontend
    participant GW as API Gateway
    participant NOTIF as Notification Service
    participant DB as Notification DB
    participant KAFKA as Kafka

    HR->>FE: View employee
    FE->>GW: Request employee information
    GW-->>FE: Employee information

    HR->>FE: Create notification + comment
    FE->>GW: Create notification
    GW->>NOTIF: Notification request
    NOTIF->>DB: Save notification
    DB-->>NOTIF: Saved
    NOTIF->>KAFKA: Publish notification event
    KAFKA-->>NOTIF: Event handled
    NOTIF-->>GW: Notification created
    GW-->>FE: Success
    FE-->>HR: Notification saved
```

---

## 7. Service Discovery

*Based on `08_ServiceDiscovery.png`.*

```mermaid
flowchart TD
    FE[React Frontend]
    GW[API Gateway]
    EUREKA{{Eureka Discovery Service}}

    subgraph Backend["Backend Services"]
        AUTH[Authentication Service]
        PROFILE[UserProfile Service]
        EMP[Employee Service]
        NOTIF[Notification Service]
    end

    SURVEY[/External Survey API/]
    KAFKA[[Kafka]]
    NOTIFDB[(Notification DB)]

    FE -->|request| GW
    GW -->|request| AUTH
    GW -->|request| PROFILE
    GW -->|request| EMP
    GW -->|request| NOTIF

    GW -.->|discovers| EUREKA
    AUTH -.->|registers| EUREKA
    PROFILE -.->|registers| EUREKA
    EMP -.->|registers| EUREKA
    NOTIF -.->|registers| EUREKA

    EMP -->|queries| SURVEY
    NOTIF -->|publishes event| KAFKA
    NOTIF --> NOTIFDB

    classDef frontend fill:#99f6e4,stroke:#0f766e,color:#134e4a
    classDef gateway fill:#fed7aa,stroke:#c2410c,color:#7c2d12
    classDef service fill:#bae6fd,stroke:#0369a1,color:#0c4a6e
    classDef eureka fill:#e5e7eb,stroke:#4b5563,stroke-dasharray: 4 3,color:#1f2937
    classDef external fill:#bbf7d0,stroke:#15803d,color:#14532d
    classDef kafka fill:#ddd6fe,stroke:#6d28d9,color:#3b0764
    classDef db fill:#dcfce7,stroke:#166534,color:#14532d

    class FE frontend
    class GW gateway
    class AUTH,PROFILE,EMP,NOTIF service
    class EUREKA eureka
    class SURVEY external
    class KAFKA kafka
    class NOTIFDB db
```

The dashed hexagon makes it visually explicit: Eureka sits beside the request path (registration/discovery only) rather than inside it — solid arrows are the only ones carrying actual traffic.

---

## 8. Database Ownership

*Based on `09_DatabaseOwnership.png`.*

```mermaid
flowchart LR
    AUTH[Authentication Service] -->|owns| AUTHDB[(Authentication DB<br/>Credentials)]
    PROFILE[UserProfile Service] -->|owns| PROFILEDB[(User Profile DB<br/>Personal Details)]
    EMP[Employee Service] -->|queries, does not own| SURVEY[/External Survey API<br/>Employee Records/]
    NOTIF[Notification Service] -->|owns| NOTIFDB[(Notification DB<br/>Notifications + Comments)]

    classDef service fill:#bae6fd,stroke:#0369a1,color:#0c4a6e
    classDef db fill:#dcfce7,stroke:#166534,color:#14532d
    classDef external fill:#bbf7d0,stroke:#15803d,color:#14532d

    class AUTH,PROFILE,EMP,NOTIF service
    class AUTHDB,PROFILEDB,NOTIFDB db
    class SURVEY external
```

Each service owns exactly one database, with one exception: Employee Service holds no database of its own — employee records live in the external Survey API, which it queries rather than owns. No tables, schemas, or databases beyond these four are implied.

---

## 9. Complete User Journey

A concise, user-focused summary of the end-to-end journey through the system.

```mermaid
flowchart LR
    Guest([Guest]) -->|Register| HR([HR])
    HR -->|Login| Search[Employee / Search]
    Search --> Attrition[Attrition Analysis]
    Attrition --> Notify[Notification]

    classDef actor fill:#f5d0fe,stroke:#701a75,color:#3b0764
    classDef step fill:#bae6fd,stroke:#0369a1,color:#0c4a6e

    class Guest,HR actor
    class Search,Attrition,Notify step
```

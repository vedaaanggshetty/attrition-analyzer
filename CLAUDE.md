# Attrition Analyzer

## Project Overview

Attrition Analyzer is a microservices-based application that helps HR users analyze employee attrition and create notifications based on employee information.

Guest users have limited access to the application.

Employee data is obtained from the external Survey API provided in the case study.

## Project Scope

The finalized Attrition Analyzer backlog consists of 21 approved user stories.

Current functional areas:

* Account & Authentication
* Profile Management
* Employee Data
* Attrition Analysis
* Notification Management
* Guest Experience

Attrition analysis terminology:

* Department
* Job Role
* Compensation
* Demographics
* Work-Life Balance
* Career Progression

Do not revert to earlier terminology (e.g. Salary/Overtime) or an earlier 19-story version of the backlog.

## User Types

There are only two user types:

* Guest
* HR User

Intended flow: Guest → Register → HR User.

Guests have limited access according to the finalized requirements and project documentation. Do not invent additional Guest permissions or restrictions, and do not introduce additional roles beyond Guest and HR User.

## Technology Stack

* Java 21
* Spring Boot 4.0.8
* Spring Cloud 2025.1.3
* Maven
* React
* MySQL
* Kafka
* Eureka Service Discovery
* Spring Cloud Gateway
* Spring Security + JWT
* Docker / Docker Compose
* Git

## Microservices

### discovery-service

Eureka Service Discovery.

### api-gateway

Single entry point for the frontend.
Responsible for routing requests and later JWT/security handling.

### authentication-service

Responsible for:

* Login
* Logout
* Password reset
* JWT authentication
* User credentials

### user-profile-service

Responsible for:

* User registration/profile information
* Viewing profile
* Updating profile

### employee-service

Responsible for:

* Fetching employee data from the Survey API
* Employee search/filtering
* Employee details
* Attrition analysis

### notification-service

Responsible for:

* Creating notifications
* Adding comments
* Viewing notifications
* Deleting notifications

### frontend

React (Vite) application used by HR and Guest users, maintained as a separate project from the backend microservices.

## Architecture

Frontend
→ API Gateway
→ Microservices

Eureka is used for service discovery.

Employee Service communicates with the external Survey API.

Kafka will be used where asynchronous communication provides a clear benefit, particularly for the notification flow.

Do not use Kafka for simple synchronous request/response operations.

## Project Structure

Each backend microservice is an independent Spring Boot Maven project.

Do not create a single multi-module Spring Boot application unless explicitly requested.

## Documentation

* `README.md` — GitHub project overview.
* `docs/technical-plan.md` — technical implementation plan.
* `docs/project-flow.md` — Mermaid-based architecture and project flow documentation.

Do not create additional documentation files unless explicitly requested.

## Development Rules

1. Inspect existing code before making changes.
2. Do not overwrite existing work without understanding it.
3. Implement one story/phase at a time.
4. Do not implement future features prematurely.
5. Keep implementations simple and understandable.
6. Follow existing project conventions.
7. Avoid unnecessary dependencies.
8. Avoid unnecessary design patterns.
9. Follow SOLID principles where appropriate.
10. Keep each service responsible for its own domain.

## Database Rules

Each service should own its own data.

Do not create a shared database between microservices unless explicitly approved.

Never hard-code database credentials.

Keep secrets and local configuration outside Git.

## API Rules

Use REST for normal synchronous communication.

Use Feign for appropriate internal synchronous service-to-service communication.

Use Kafka only when asynchronous communication is actually useful.

Use DTOs when communicating across service boundaries.

Do not expose external Survey API models directly to the frontend.

## Security Rules

* Passwords must always be securely hashed.
* Never store plaintext passwords.
* Never log passwords.
* Never log JWT tokens.
* Never commit secrets or credentials.
* JWT authentication will be implemented through Spring Security.

## Testing Rules

Every implemented feature should have appropriate automated tests.

At minimum, add unit tests for business logic and endpoint tests for important APIs.

Run tests after making changes.

Never claim that tests passed unless they were actually executed.

## Claude Code Rules

Before implementing a significant change:

1. Inspect the relevant code.
2. Explain the proposed approach.
3. Identify files that will be changed.
4. Wait for approval for major architectural changes.

When implementing a user story:

1. Understand the acceptance criteria.
2. Implement only that story.
3. Add/update tests.
4. Run the relevant tests.
5. Review the changes.
6. Report what was changed and what was tested.

Do not implement multiple unrelated stories at once.

Do not make destructive database, Git, Docker, or filesystem changes without asking.

## Scope Control

The current backlog consists of the approved Attrition Analyzer user stories.

Do not invent major new functionality.

If a requirement is unclear, ask before making a major architectural decision.

## Source of Truth

* For requirements: use the finalized Product Backlog.
* For architecture and flows: use `docs/project-flow.md` and the existing diagrams.
* For technical implementation planning: use `docs/technical-plan.md`.

If these sources conflict, stop and report the conflict instead of silently choosing one.

Do not invent requirements. Do not implement future stories prematurely.

## Git Safety

Never do the following without explicit approval:

* Force push
* Reset or rebase shared history
* Delete project files
* Untrack files
* Modify `.gitignore` destructively

Do not commit or push unless explicitly asked.

## Current Development Phase

We are currently setting up the project foundation.

Do not implement business functionality until the foundation is working.

The initial foundation consists of:

* Independent Spring Boot service projects
* Eureka Discovery Service
* Eureka client registration for every service
* API Gateway foundation
* Service ports/configuration
* Basic health checks
* Basic documentation

Verify that all services can start and register correctly before moving on.

After the foundation is verified, development will proceed story by story according to `docs/technical-plan.md`.

React
  ↓
API Gateway
  ↓
┌───────────────────────────────┐
│ Authentication                │
│ User Profile                  │
│ Employee                      │
│ Notification                  │
└───────────────────────────────┘
       ↓             ↓
     MySQL         Kafka
                     ↓
              Notification
                     
Employee → Survey API

## Design Idea for FrontEnd 
use this only for design reference  
**Create a single-page landing hero section for a creative agency called "Alwayzz" with a React + Vite + Tailwind CSS setup. Use custom CSS (not Tailwind utilities) for all styling. The design should be minimal, clean, black-and-white, with tight negative letter-spacing throughout.**
---
### Fonts (loaded via Google Fonts in index.html)
```
Inter: weights 400, 500, 600, 700
Source Serif 4: weights 400, 600 (both normal and italic)
```
Preconnect to `fonts.googleapis.com` and `fonts.gstatic.com`.
---
### CSS Variables
```css
--bg: #ffffff;
--text: #0a0a0a;
--muted: #6b6b6b;
--button-bg: #0a0a0a;
--button-text: #ffffff;
--border-soft: rgba(0, 0, 0, 0.08);
--green: #17c964;
```
---
### Components
**1. Navbar (fixed top, z-index 100)**
- Padding: `19px 36px`, max-width `1200px` centered.
- Left: Logo text "Alwayzz" in `Source Serif 4`, 30px, weight 600, **italic**, letter-spacing `-0.08em`, with a registered trademark symbol in Inter 14px weight 600.
- Right: "Menu" pill button (black bg, white text, rounded-full, 14px weight 500, Inter) with a `ChevronUp` icon (16px) from lucide-react.
- Full-screen drawer overlay on click: white bg, fade transition 0.4s. Nav links centered vertically at 48px weight 500, letter-spacing `-0.04em`. Links: Projects, Plans, Team, FAQs, Get in Touch. Footer with copyright.
**2. Hero Section**
- Min-height: `850px`, padding: `160px 36px`, centered flex column.
- **Background image** (via `::before` pseudo-element, covers full section):
  ```
  https://images.higgs.ai/?default=1&output=webp&url=https%3A%2F%2Fd8j0ntlcm91z4.cloudfront.net%2Fuser_38xzZboKViGWJOttwIXH07lWA1P%2Fhf_20260626_041422_4a459e05-abce-4150-9fb7-4ededc423cd1.png&w=1280&q=85
  ```
  Background-size: cover, background-position: center.
- **Curved line animations** (decorative):
  - 20 lines on left side, 20 on right side, absolutely positioned.
  - Each line is a tall rectangle with one-sided border-radius (80%) and `2.5px solid #FCFAF8` border.
  - Left lines: no left border, radius on right. Right lines: no right border, radius on left.
  - Staggered `animationDelay: i * 0.25s`, widths from 60px increasing by 10px per line.
  - Animation: `line-pulse` 5s ease-in-out infinite (fade in to 0.9 opacity, then back to 0 with slight scale).
  - On mobile (<810px): hide side lines, show top horizontal lines instead (same animation, horizontal orientation with bottom border-radius).
- **Ticker row** (max-width 500px, height 36px):
  - Horizontal marquee scrolling left over 30s, linear, infinite.
  - Items: "Brand Identity", "App Development", "Visual Design", "Creative Video", "Iconography"
  - Each item: 13px, weight 500, color `var(--muted)`, padding `6px 14px`, rounded-full, background `rgb(251, 251, 251)`.
  - Marquee has edge fade mask: `linear-gradient(90deg, transparent 0%, black 12%, black 88%, transparent 100%)`.
  - 4x duplicated rows for seamless loop.
- **Title**:
  ```
  Premium creative <span class="serif italic">alwayzz</span><sup style registered mark> on demand.
  ```
  - Max-width: 550px, font-size: 82px, line-height: 1.03, letter-spacing: `-0.07em`, weight 600, centered.
  - The word "alwayzz" uses `Source Serif 4`, italic, weight 600, letter-spacing `-0.08em`.
  - The registered mark: Inter, 24px, weight 600, vertical-align super.
- **Subtitle**:
  ```
  A flexible design partnership for founders, brands, and agencies who want top craft delivered on their timeline.
  ```
  - Max-width: 476px, 17px, line-height 1.45, weight 400, color `var(--muted)`, centered.
- **CTA row** (flex, gap 16px, centered):
  - **Primary button** "View Plans": height 56px, padding `18px 30px`, rounded-full, black bg, white text, 15px weight 600 Inter. Hover: translateY(-1px) + box-shadow `0 4px 20px rgba(0,0,0,0.12)`.
  - **Book button** "Chat for 15 minutes": white bg, `4px solid rgb(248,248,248)` border, rounded-full, padding `8px 24px 8px 8px`. Contains:
    - Avatar image (40px circle): `https://framerusercontent.com/images/hfneFL6CHBi5BnNvCeOaqU9HqE4.png`
    - Text stack: primary "Chat for 15 minutes" (14px, weight 600, black) and secondary "Pick a slot" (12px, weight 500, `rgb(152,152,152)`) with a green dot (`rgb(29, 204, 93)`, 8px circle).
- **Progressive blur** at bottom: absolute, full width, height 178px, gradient from transparent to `rgba(255,255,255,0.4)` at 40% to solid white.
**3. TrustedBy Section**
- Padding: 36px, max-width 1200px centered.
- Left: label "Partnered with top-tier companies globally" (max-width 163px, 14px, weight 500, muted color).
- Right: horizontal marquee (30s) of company names styled as text logos (16px, weight 600, black, each with distinct font-family):
  - Airbnb (Cedarville Cursive, 700), Shopify (system-ui, 800), Notion (Georgia, 500), Linear (Inter, 600), Webflow (Inter, 700), Figma (system-ui, 600), Slack (Georgia, 700), Stripe (system-ui, 800), Vercel (Inter, 600), Framer (Source Serif 4, 600).
- Same edge-fade marquee mask as ticker.
---
### Responsive Breakpoints
- **< 1200px**: Hero padding `140px 32px`, title clamp(60px, 8vw, 72px), navbar padding 32px, drawer links 40px.
- **< 810px**: Hero min-height 760px, padding `120px 24px 96px`. Background image rotated 90deg to fill portrait viewport. Side curved lines hidden, top horizontal lines shown. Title clamp(44px, 13vw, 52px). CTA buttons stack vertically full-width (max 320px). Trusted section stacks vertically. Drawer links 32px. Navbar padding 20px.
---
### Key Animation Keyframes
```css
@keyframes marquee-left {
  from { transform: translateX(0); }
  to { transform: translateX(-50%); }
}
@keyframes line-pulse {
  0% { opacity: 0; transform: scale(1); }
  15% { opacity: 0.9; }
  70% { opacity: 0.4; }
  100% { opacity: 0; transform: scale(0.85); }
}
```
---
### CloudFront Video/Image URL
The hero background image URL (exact):
```
https://images.higgs.ai/?default=1&output=webp&url=https%3A%2F%2Fd8j0ntlcm91z4.cloudfront.net%2Fuser_38xzZboKViGWJOttwIXH07lWA1P%2Fhf_20260626_041422_4a459e05-abce-4150-9fb7-4ededc423cd1.png&w=1280&q=85
```
The book button avatar URL (exact):
```
https://framerusercontent.com/images/hfneFL6CHBi5BnNvCeOaqU9HqE4.png
```
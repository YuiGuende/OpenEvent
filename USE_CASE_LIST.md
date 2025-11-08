# OpenEvent - Complete Use Case List

## Authentication & User Management

| ID | Use Case | Actor | Description |
|----|----------|-------|-------------|
| 10 | Sign In | Customer, Department, Host, Admin | User logs into the system using credentials (email/username and password) or OAuth (Gmail). |
| 11 | Sign Out | Customer, Department, Host, Admin | User logs out of the system to end their session. |
| 12 | Register | Customer | Customer creates a new account by providing personal information and credentials. |
| 13 | Forgot Password | Customer, Department, Admin | User resets their password via email recovery process. |
| 14 | View Profile | Customer, Department, Host, Admin | User views their profile details (name, contact info, role, points). |
| 15 | Change Password | Customer, Department, Admin | User updates their account password for security. |
| 16 | Delete Account | Admin | Admin permanently deletes a user account from the system. |
| 17 | Update Profile | Customer, Department, Host | User edits their profile information (contact details, preferences). |
| 43 | Ban User Accounts | Admin | Admin suspends or bans user accounts for policy violations. |

## Event Management

| ID | Use Case | Actor | Description |
|----|----------|-------|-------------|
| 01 | View List Events | Customer, Guest | Views a list of all events (public events that users can join or want to join). |
| 03 | Update Event Details | Department, Host | Can update details of an event (speaker, ticket, schedule, location, etc.). |
| 04 | Create Events | Host, Department | Host creates event with day and time, lineup, location, ticket types, and other details. |
| 05 | Design Event Pages | Host, Department | Host designs and customizes event pages with images, descriptions, and themes. |
| 07 | Filter Event List | Customer, Admin, Guest | Filter events by criteria (date, type, ticket availability, status) for easier discovery. |
| 20 | Search for Event | Customer, Guest | User searches for events using keywords, filters by type, date range. |
| 22 | View Service/Event Details | Customer, Guest | User views detailed information about a specific event (type, price, description, speakers, schedule). |
| 29 | View Event List | Customer, Guest | Customer views a list of available events in the system. |
| 30 | View Event Details | Customer, Guest | Customer views detailed information about a specific event. |
| 33 | View Event Status | Host | Host checks the status of their event (DRAFT, PENDING, PUBLIC, ONGOING, CANCEL, etc.). |
| 26 | Cancel Event | Customer, Host | Customer or Host cancels an event. If tickets were purchased, refunds are processed automatically. |
| 02 | Reject/Approve Event | Department | Department approves or rejects event requests from Hosts. |

## Ticket & Order Management

| ID | Use Case | Actor | Description |
|----|----------|-------|-------------|
| 27 | Order Products/Tickets | Customer | Customer places an order for ticket products from the event. |
| 08 | View Purchased Tickets | Customer | Customer views purchased ticket details and tickets they have bought. |
| 31 | Add Ticket to Cart | Customer | Customer selects and adds tickets to their cart for purchase. |
| 32 | Delete Product from Cart | Customer | Customer removes products/tickets from their shopping cart. |
| 34 | View Order Details | Customer | Customer views detailed information about a specific order (items, total cost, status). |
| 35 | Give Order Feedback | Customer | Customer submits feedback or rating for a completed order. |
| 36 | View Refund Tickets | Admin | Admin views list of refunded tickets and refund requests. |
| 50 | Request Refund | Customer | Customer requests refund for cancelled event or order. System processes refund automatically. |

## Event Participation & Attendance

| ID | Use Case | Actor | Description |
|----|----------|-------|-------------|
| 06 | Attend the Event | Customer | Customer can attend events with two roles: volunteer or participant. |
| 09 | View My Events | Customer | View a list of personal events attended with details (date, time, speaker/ticket, notes). |
| 28 | Check In/Out Event | Customer | Manage attendee entry and exit to authenticate access, record attendance, and collect accurate attendance data using QR codes. |
| 21 | View Attendance List | Host, Department | Views a list of attendees who have joined the events. |
| 23 | Give Service/Event Feedback | Guest, Customer | Submits feedback or rating for an event service received (via checkout form). |

## Payment & Wallet

| ID | Use Case | Actor | Description |
|----|----------|-------|-------------|
| 51 | Process Payment | Customer | Customer processes payment for tickets using PayOS payment gateway. |
| 52 | View Payment History | Customer | Customer views their payment transaction history. |
| 53 | Request Payout | Host | Host requests withdrawal of earnings from their wallet to bank account. |
| 54 | View Wallet Balance | Host | Host views their wallet balance and transaction history. |

## Communication & Notifications

| ID | Use Case | Actor | Description |
|----|----------|-------|-------------|
| 18 | Send Message | Customer, Host, Department | User sends notification/message to other users (especially Host to event participants). |
| 55 | View Notifications | Customer, Host, Department, Admin | User views notifications received from the system. |

## AI & Chatbot

| ID | Use Case | Actor | Description |
|----|----------|-------|-------------|
| 19 | Chat with AI Agent | Customer, Host, Department | Entities interact with a Chatbot to receive automated support, answer FAQs, provide service information, and guide users. |
| 42 | Consult AI for Event Suggestions | Host, Department | Host/Department consults AI for event suggestions, promotional strategies, and recommendations. |

## Vouchers & Promotions

| ID | Use Case | Actor | Description |
|----|----------|-------|-------------|
| 41 | Manage Discount/Promotions | Host, Department | Hosts and Departments create and manage discount codes or promotions to attract attendees and drive ticket sales. |
| 56 | Validate Voucher | Customer | Customer validates and applies voucher codes during checkout. |

## Points & Rewards

| ID | Use Case | Actor | Description |
|----|----------|-------|-------------|
| 25 | Earn Points After Event | Customer | System rewards attendees with points after each event participation to encourage return and future participation. |
| 57 | View Points Balance | Customer | Customer views their current points balance and points history. |

## Statistics & Reporting

| ID | Use Case | Actor | Description |
|----|----------|-------|-------------|
| 24 | View Event Statistics | Host, Department | Host and Department access dashboard to view key metrics and statistics related to events (revenue, attendance, tickets sold). |
| 37 | View Admin Dashboard | Admin | Provides Admins with an overview and tools to monitor and manage entire operations of the ticketing and event organization platform. |
| 39 | Monitor Event Revenue | Host, Department | Provides organizers with visual tools to track and analyze financial situation of events in real time. |
| 48 | View Request Reports | Admin, Department | Admin/Department views reports summarizing request data (approvals, rejections, processing times). |
| 49 | View Order/Sales Statistics | Admin | Admin views statistics on ticket sales (revenue, popular events, order status distribution). |

## Department Management

| ID | Use Case | Actor | Description |
|----|----------|-------|-------------|
| 46 | Create Department | Admin | Admin creates accounts for new departments. |
| 47 | Delete Department | Admin | Admin deletes department accounts from the system. |
| 58 | View Department Dashboard | Department | Department views dashboard with statistics about their events, requests, and orders. |
| 59 | Manage Articles | Department | Department creates, edits, publishes, and manages articles/news related to events. |

## Additional Features

| ID | Use Case | Actor | Description |
|----|----------|-------|-------------|
| 40 | Export Attendee List | Host, Department | Organizers download the attendee list as a file (CSV/Excel) for offline use or integration with other tools. |
| 60 | View Event Dashboard | Host | Host views detailed dashboard for a specific event with statistics, orders, attendance, and revenue. |
| 61 | Submit Event Form | Customer | Customer submits check-in forms or feedback forms for events. |
| 62 | View Event Forms | Host, Department | Host/Department views and manages event forms (check-in, feedback forms). |
| 38 | View Leaderboard | Guest, Customer | System displays leaderboard of most active participants/customers (if implemented). |

## Notes

- **Use Case 32 (Delete from Cart)**: The system currently uses a simplified direct order creation flow, so cart management is minimal.
- **Use Case 38 (Leaderboard)**: This feature may not be fully implemented in the current codebase.
- **Use Case 44-45 (Create/Delete Medical Services)**: These appear to be from a different domain (medical clinic) and do not apply to this event management system.
- **Use Case 22 (View Service Details)**: In the context of this system, this refers to viewing event details.



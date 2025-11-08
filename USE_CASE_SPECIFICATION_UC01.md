# UC 01: View List Events
## Use Case Specification

---

## Use Case ID and Name
**UC-01: View List Events**

**Created By:** System Analyst  
**Date Created:** December 2024  
**Last Updated:** December 2024

---

## Primary Actor
**Customer, Guest**

## Secondary Actors
**None**

---

## Trigger
A customer or guest wants to view the list of available events in the system to discover events they can join or are interested in.

---

## Description
This use case allows customers and guests (non-authenticated users) to view a list of all available events in the system. The list displays events with their basic information including title, image, date, time, location, event type, and status. Users can browse through events to find ones they want to attend or learn more about.

---

## Preconditions
- **PRE-1:** The system is operational and accessible.
- **PRE-2:** At least one event exists in the system with status PUBLIC or ONGOING (for guests).
- **PRE-3:** For authenticated customers, they have a valid account (optional for viewing, but may affect personalized content).

---

## Post-conditions
- **POST-1:** List of available events is displayed to the user.
- **POST-2:** User can view event details by clicking on an event.
- **POST-3:** User can filter or search events based on their preferences (if applicable).

---

## Normal Flow
1. Customer or Guest navigates to the home page or events listing page.
2. System retrieves all available events from the database (PUBLIC and ONGOING status for guests; may include more for authenticated customers).
3. System displays the list of events with:
   - Event title
   - Event image/thumbnail
   - Event date and time
   - Event location
   - Event type (Music, Festival, Workshop, Competition, etc.)
   - Event status
   - Ticket availability (if applicable)
4. User browses through the event list.
5. User can click on an event to view detailed information (navigates to View Event Details use case).
6. System records the view activity (for analytics purposes).

---

## Alternative Flows

### 1-AF: No Events Available
- **a.** System displays: "No events found. Please check back later."
- **b.** System may show upcoming events or suggest browsing by category.
- **c.** The use case ends.

### 2-AF: Filter Events
- **a.** User applies filters (event type, date range, status).
- **b.** System retrieves and displays filtered event list.
- **c.** Continue from step 4 of Normal Flow.

### 3-AF: Search Events
- **a.** User enters search keywords in the search box.
- **b.** System searches events by title, description, or keywords.
- **c.** System displays matching events.
- **d.** Continue from step 4 of Normal Flow.

### 4-AF: Paginated Results
- **a.** If the event list is large, system displays events in pages.
- **b.** User can navigate to next/previous pages.
- **c.** Continue from step 4 of Normal Flow.

---

## Exceptions

### 1-EF: Database Connection Failure
- **a.** System displays an error message: "Unable to load events. Please try again later."
- **b.** System logs the error for administrator review.
- **c.** The use case ends.

### 2-EF: Invalid Filter Parameters
- **a.** System validates filter parameters.
- **b.** If invalid, system displays: "Invalid filter criteria. Please adjust your filters."
- **c.** System displays default event list.
- **d.** Continue from step 4 of Normal Flow.

### 3-EF: Network Timeout
- **a.** System detects network timeout during data retrieval.
- **b.** System displays: "Connection timeout. Please refresh the page."
- **c.** User can retry the operation.

---

## Priority
**High**

---

## Frequency of Use
**Very High** (used continuously by all users to discover events)

---

## Business Rules

- **BR-1:** Only events with status PUBLIC or ONGOING are visible to guests.
- **BR-2:** Authenticated customers may see additional events based on their preferences or past attendance.
- **BR-3:** Events must be displayed in chronological order (upcoming events first) by default.
- **BR-4:** Event list must be updated in real-time to reflect current availability.
- **BR-5:** System must ensure event data integrity and prevent displaying expired or cancelled events as available.
- **BR-6:** Event images and thumbnails must be optimized for fast loading.
- **BR-7:** Pagination must be implemented for lists exceeding 20 events per page.

---

## Special Requirements

- **SR-1:** The system must support responsive design for mobile and desktop viewing.
- **SR-2:** Event list must load within 2 seconds under normal network conditions.
- **SR-3:** System should cache frequently accessed event data to improve performance.
- **SR-4:** Event list should support lazy loading for better user experience.

---

## Assumptions
- Users have basic internet connectivity.
- Users understand how to navigate web interfaces.
- Event data is properly maintained and up-to-date in the database.

---

## Other Information

### Related Use Cases
- **UC-07:** Filter Event List
- **UC-20:** Search for Event
- **UC-30:** View Event Details
- **UC-29:** View Event List

### Extensions
- Advanced filtering options (by price range, location, popularity).
- Personalized event recommendations based on user's browsing history.
- Export event list functionality (PDF, Excel).
- Social sharing of event listings.

### Technology Notes
- Frontend: Thymeleaf templates, JavaScript
- Backend: Spring Boot, Java
- Database: MySQL
- Caching: May use Redis or in-memory caching for performance

---

## Test Scenarios

### TC-01: View Events as Guest
1. Open browser as guest user.
2. Navigate to home page.
3. **Expected:** List of public events is displayed.

### TC-02: View Events as Authenticated Customer
1. Log in as customer.
2. Navigate to events page.
3. **Expected:** List of available events (including personalized recommendations) is displayed.

### TC-03: No Events Available
1. Navigate to events page when no events exist.
2. **Expected:** "No events found" message is displayed.

### TC-04: Filter Events by Type
1. Navigate to events page.
2. Select "Music" from event type filter.
3. **Expected:** Only music events are displayed.

---

## Status
**Implemented**

---

## Version History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | December 2024 | System Analyst | Initial specification |



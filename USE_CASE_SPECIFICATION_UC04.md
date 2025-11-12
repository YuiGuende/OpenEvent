# UC 04: Create Events
## Use Case Specification

---

## Use Case ID and Name
**UC-04: Create Events**

**Created By:** System Analyst  
**Date Created:** December 2024  
**Last Updated:** December 2024

---

## Primary Actor
**Host, Department**

## Secondary Actors
**Department** (for approval)

---

## Trigger
A Host wants to create a new event to organize and sell tickets to customers.

---

## Description
This use case allows Hosts to create a new event by providing comprehensive event information including event type (Music, Festival, Workshop, Competition), date and time, location, lineup/speakers, ticket types and pricing, capacity, description, images, and other relevant details. After creation, the event is submitted for Department approval before it becomes publicly visible.

---

## Preconditions
- **PRE-1:** The Host is logged into the system with valid credentials.
- **PRE-2:** The Host has a valid Host account (not just a Customer account).
- **PRE-3:** The Host has necessary permissions to create events.
- **PRE-4:** The system has at least one Department configured for event approval.

---

## Post-conditions
- **POST-1:** A new event is created in the system with status DRAFT or PENDING.
- **POST-2:** The event is submitted to the Department for approval.
- **POST-3:** The Host can view the created event in their event management dashboard.
- **POST-4:** The Host receives a notification about the event creation and pending approval.

---

## Normal Flow
1. Host logs into the system.
2. Host navigates to "Create Event" page or clicks "Create New Event" button.
3. System displays the event creation form with required fields:
   - Event Type (Music, Festival, Workshop, Competition, Others)
   - Event Title
   - Description
   - Start Date and Time
   - End Date and Time
   - Location/Place
   - Capacity
   - Event Image
4. Host fills in basic event information.
5. System validates the entered data (required fields, date format, etc.).
6. Host adds event details:
   - Speakers/Performers (if applicable)
   - Schedule/Lineup
   - Ticket Types and Pricing
   - Event Images/Gallery
7. Host submits the event creation form.
8. System creates the event record with status DRAFT or PENDING.
9. System creates a request for Department approval.
10. System sends notification to the assigned Department.
11. System displays success message: "Event created successfully. Waiting for approval."
12. System redirects Host to their event management dashboard.

---

## Alternative Flows

### 1-AF: Save as Draft
- **a.** Host clicks "Save as Draft" instead of "Submit for Approval".
- **b.** System saves event with status DRAFT.
- **c.** Host can edit and submit later.
- **d.** The use case ends.

### 2-AF: Add Multiple Ticket Types
- **a.** Host adds multiple ticket types (e.g., VIP, Standard, Early Bird).
- **b.** For each ticket type, Host specifies:
   - Ticket name
   - Price
   - Quantity available
   - Sale start/end dates (optional)
   - Discount (optional)
- **c.** System validates ticket type data.
- **d.** Continue from step 6 of Normal Flow.

### 3-AF: Upload Event Images
- **a.** Host uploads multiple images for the event.
- **b.** System uploads images to cloud storage (Cloudinary).
- **c.** System generates image URLs and stores them.
- **d.** Host can set one image as the main event image.
- **e.** Continue from step 6 of Normal Flow.

### 4-AF: Add Speakers/Performers
- **a.** Host adds speakers or performers to the event.
- **b.** For each speaker, Host provides:
   - Name
   - Bio/Description
   - Photo (optional)
   - Role/Title
- **c.** System validates speaker information.
- **d.** Continue from step 6 of Normal Flow.

### 5-AF: Add Event Schedule
- **a.** Host creates a detailed schedule/lineup for the event.
- **b.** For each schedule item, Host provides:
   - Time slot
   - Activity/Title
   - Description
   - Speaker/Performer (optional)
- **c.** System validates schedule data.
- **d.** Continue from step 6 of Normal Flow.

### 6-AF: Select Existing Location
- **a.** Host selects location from existing places in the system.
- **b.** System displays list of available places.
- **c.** Host selects a place.
- **d.** Continue from step 4 of Normal Flow.

### 7-AF: Create New Location
- **a.** Host creates a new location/place.
- **b.** Host provides place details (name, address, coordinates).
- **c.** System creates the place record.
- **d.** Host uses the new place for the event.
- **e.** Continue from step 4 of Normal Flow.

---

## Exceptions

### 1-EF: Validation Error
- **a.** System detects invalid or missing required fields.
- **b.** System displays error messages indicating which fields need correction.
- **c.** Host corrects the errors.
- **d.** Continue from step 4 of Normal Flow.

### 2-EF: Database Error
- **a.** System encounters database error during event creation.
- **b.** System displays: "Unable to create event. Please try again later."
- **c.** System logs the error for administrator review.
- **d.** The use case ends.

### 3-EF: Image Upload Failure
- **a.** System fails to upload event images.
- **b.** System displays warning: "Image upload failed. You can add images later."
- **c.** Host can proceed without images or retry upload.
- **d.** Continue from step 8 of Normal Flow.

### 4-EF: Invalid Date/Time
- **a.** System detects that end date is before start date.
- **b.** System displays: "End date must be after start date."
- **c.** Host corrects the dates.
- **d.** Continue from step 4 of Normal Flow.

### 5-EF: Invalid Ticket Pricing
- **a.** System detects invalid ticket pricing (negative values, etc.).
- **b.** System displays: "Invalid ticket price. Please enter a valid amount."
- **c.** Host corrects the pricing.
- **d.** Continue from step 6 of Normal Flow.

### 6-EF: Network Timeout
- **a.** System detects network timeout during form submission.
- **b.** System displays: "Connection timeout. Please check your internet connection and try again."
- **c.** System may save draft automatically if possible.
- **d.** Host can retry submission.

---

## Priority
**High**

---

## Frequency of Use
**Medium** (used when Hosts want to organize new events)

---

## Business Rules

- **BR-1:** Only users with Host role can create events.
- **BR-2:** Events must have a unique title (or system-generated unique identifier).
- **BR-3:** Event start date must be in the future (at least 24 hours from creation).
- **BR-4:** Event end date must be after start date.
- **BR-5:** Events must be assigned to a Department for approval.
- **BR-6:** Ticket prices must be non-negative values.
- **BR-7:** Total ticket quantity must not exceed event capacity.
- **BR-8:** Events in DRAFT status are only visible to the Host who created them.
- **BR-9:** Events must have at least one ticket type defined.
- **BR-10:** Event images must meet size and format requirements (specified in system configuration).

---

## Special Requirements

- **SR-1:** The system must support real-time form validation.
- **SR-2:** Event creation form must be responsive for mobile and desktop.
- **SR-3:** System must support rich text editing for event descriptions.
- **SR-4:** Image upload must support drag-and-drop functionality.
- **SR-5:** System must auto-save draft events periodically to prevent data loss.
- **SR-6:** Event creation must complete within 30 seconds under normal conditions.

---

## Assumptions
- Hosts have basic knowledge of event management and the information required.
- Hosts have access to event images and necessary media files.
- Department approval workflow is properly configured in the system.
- Hosts understand the difference between draft and submission for approval.

---

## Other Information

### Related Use Cases
- **UC-02:** Reject/Approve Event (Department approves the created event)
- **UC-03:** Update Event Details (Host can update event after creation)
- **UC-05:** Design Event Pages (customize event appearance)
- **UC-24:** View Event Statistics (view statistics after event is active)

### Extensions
- Bulk event creation (import from CSV/Excel).
- Event templates for quick creation.
- Integration with external calendar systems.
- Automatic event reminders and notifications.

### Technology Notes
- Frontend: Thymeleaf templates, JavaScript (form validation, image upload)
- Backend: Spring Boot, Java
- Database: MySQL (Event, TicketType, Speaker, Schedule, Place tables)
- File Storage: Cloudinary for image management
- Validation: Spring Validation framework

---

## Test Scenarios

### TC-01: Create Music Event Successfully
1. Log in as Host.
2. Navigate to "Create Event".
3. Select "Music" as event type.
4. Fill in all required fields.
5. Add ticket types.
6. Submit for approval.
7. **Expected:** Event created with PENDING status, notification sent to Department.

### TC-02: Create Event with Missing Required Fields
1. Log in as Host.
2. Navigate to "Create Event".
3. Leave required fields empty.
4. Submit form.
5. **Expected:** Validation errors displayed, event not created.

### TC-03: Save Event as Draft
1. Log in as Host.
2. Fill in partial event information.
3. Click "Save as Draft".
4. **Expected:** Event saved with DRAFT status, can be edited later.

### TC-04: Create Event with Invalid Date Range
1. Log in as Host.
2. Set end date before start date.
3. Submit form.
4. **Expected:** Error message displayed, event not created.

### TC-05: Create Event with Multiple Ticket Types
1. Log in as Host.
2. Create event with 3 ticket types (VIP, Standard, Early Bird).
3. Set different prices for each.
4. Submit for approval.
5. **Expected:** Event created with all ticket types, all prices stored correctly.

---

## Status
**Implemented**

---

## Version History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | December 2024 | System Analyst | Initial specification |







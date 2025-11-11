package com.group02.openevent.controller.form;

import com.group02.openevent.ai.security.AISecurityService;
import com.group02.openevent.ai.security.RateLimitingService;
import com.group02.openevent.config.SessionInterceptor;
import com.group02.openevent.dto.form.*;
import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.form.EventForm;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.repository.IAccountRepo;
import com.group02.openevent.repository.ICustomerRepo;
import com.group02.openevent.service.EventAttendanceService;
import com.group02.openevent.service.EventFormService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = EventFormController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class)
@DisplayName("EventFormController Integration Tests")
class EventFormControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SessionInterceptor sessionInterceptor;
    @MockBean
    private EventFormService eventFormService;
    @MockBean
    private EventAttendanceService attendanceService;
    @MockBean
    private ICustomerRepo customerRepo;
    @MockBean
    private IAccountRepo accountRepo;

    private EventFormDTO registerFormDTO;
    private EventFormDTO checkinFormDTO;
    private EventFormDTO feedbackFormDTO;
    private FormResponseDTO responseDTO;

    private static final Long EVENT_ID = 1L;
    private static final Long FORM_ID = 10L;
    private static final String EMAIL = "test@example.com";
    @MockBean
    private RateLimitingService rateLimitingService;

    @MockBean
    private AISecurityService aiSecurityService;
    @BeforeEach
    void setUp() throws Exception {
        // REGISTER Form
        registerFormDTO = new EventFormDTO();
        registerFormDTO.setFormId(FORM_ID);
        registerFormDTO.setEventId(EVENT_ID);
        registerFormDTO.setFormTitle("Register Form");
        registerFormDTO.setFormType(EventForm.FormType.REGISTER);
        registerFormDTO.setQuestions(new ArrayList<>());

        // CHECKIN Form
        checkinFormDTO = new EventFormDTO();
        checkinFormDTO.setFormId(FORM_ID + 1);
        checkinFormDTO.setEventId(EVENT_ID);
        checkinFormDTO.setFormTitle("Check-in Form");
        checkinFormDTO.setFormType(EventForm.FormType.CHECKIN);
        checkinFormDTO.setQuestions(new ArrayList<>());

        // FEEDBACK Form
        feedbackFormDTO = new EventFormDTO();
        feedbackFormDTO.setFormId(FORM_ID + 2);
        feedbackFormDTO.setEventId(EVENT_ID);
        feedbackFormDTO.setFormTitle("Feedback Form");
        feedbackFormDTO.setFormType(EventForm.FormType.FEEDBACK);
        feedbackFormDTO.setQuestions(new ArrayList<>());

        // Response DTO
        responseDTO = new FormResponseDTO();
        responseDTO.setResponseId(1L);
        responseDTO.setFormId(FORM_ID);
        responseDTO.setCustomerId(1L);
        responseDTO.setCustomerName("Test User");
        responseDTO.setCustomerEmail(EMAIL);
        responseDTO.setQuestionText("Test Question");
        responseDTO.setResponseValue("Test Answer");
        responseDTO.setFormType(EventForm.FormType.REGISTER);

        // Mock SessionInterceptor để cho phép tất cả request đi qua
        when(sessionInterceptor.preHandle(any(), any(), any())).thenReturn(true);
    }

    @Nested
    @DisplayName("showSelectFormType Tests")
    class ShowSelectFormTypeTests {
        @Test
        @DisplayName("TC-01: Show select form type page successfully")
        void showSelectFormType_Success() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/forms/{eventId}", EVENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(view().name("user/select-form-type"))
                    .andExpect(model().attributeExists("eventId"));
        }
    }

    @Nested
    @DisplayName("showRegisterForm Tests")
    class ShowRegisterFormTests {
        @Test
        @DisplayName("TC-02: Show register form successfully")
        void showRegisterForm_Success() throws Exception {
            // Arrange
            when(eventFormService.getActiveFormByEventIdAndType(EVENT_ID, EventForm.FormType.REGISTER))
                    .thenReturn(registerFormDTO);

            // Act & Assert
            mockMvc.perform(get("/forms/register/{eventId}", EVENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(view().name("user/feedback-form"))
                    .andExpect(model().attributeExists("form"))
                    .andExpect(model().attributeExists("eventId"));
        }

        @Test
        @DisplayName("TC-03: Show register form when form not found")
        void showRegisterForm_FormNotFound() throws Exception {
            // Arrange
            when(eventFormService.getActiveFormByEventIdAndType(EVENT_ID, EventForm.FormType.REGISTER))
                    .thenThrow(new RuntimeException("No active REGISTER form found"));

            // Act & Assert
            mockMvc.perform(get("/forms/register/{eventId}", EVENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(view().name("user/feedback-form"))
                    .andExpect(model().attributeDoesNotExist("form"))
                    .andExpect(model().attributeExists("noFormMessage"));
        }
    }

    @Nested
    @DisplayName("showCheckinForm Tests")
    class ShowCheckinFormTests {
        @Test
        @DisplayName("TC-04: Show check-in form successfully")
        void showCheckinForm_Success() throws Exception {
            // Arrange
            when(eventFormService.getActiveFormByEventIdAndType(EVENT_ID, EventForm.FormType.CHECKIN))
                    .thenReturn(checkinFormDTO);

            // Act & Assert
            mockMvc.perform(get("/forms/checkin/{eventId}", EVENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(view().name("user/feedback-form"))
                    .andExpect(model().attributeExists("form"));
        }

        @Test
        @DisplayName("TC-05: Show check-in form when form not found")
        void showCheckinForm_FormNotFound() throws Exception {
            // Arrange
            when(eventFormService.getActiveFormByEventIdAndType(EVENT_ID, EventForm.FormType.CHECKIN))
                    .thenThrow(new RuntimeException("No active CHECKIN form found"));

            // Act & Assert
            mockMvc.perform(get("/forms/checkin/{eventId}", EVENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeDoesNotExist("form"))
                    .andExpect(model().attributeExists("noFormMessage"));
        }
    }

    @Nested
    @DisplayName("showFeedbackForm Tests")
    class ShowFeedbackFormTests {
        @Test
        @DisplayName("TC-06: Show feedback form successfully")
        void showFeedbackForm_Success() throws Exception {
            // Arrange
            when(eventFormService.getActiveFormByEventIdAndType(EVENT_ID, EventForm.FormType.FEEDBACK))
                    .thenReturn(feedbackFormDTO);

            // Act & Assert
            mockMvc.perform(get("/forms/feedback/{eventId}", EVENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(view().name("user/feedback-form"))
                    .andExpect(model().attributeExists("form"));
        }

        @Test
        @DisplayName("TC-07: Show feedback form when form not found")
        void showFeedbackForm_FormNotFound() throws Exception {
            // Arrange
            when(eventFormService.getActiveFormByEventIdAndType(EVENT_ID, EventForm.FormType.FEEDBACK))
                    .thenThrow(new RuntimeException("No active FEEDBACK form found"));

            // Act & Assert
            mockMvc.perform(get("/forms/feedback/{eventId}", EVENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeDoesNotExist("form"))
                    .andExpect(model().attributeExists("noFormMessage"));
        }
    }

    @Nested
    @DisplayName("submitFeedback Tests")
    class SubmitFeedbackTests {
        @Test
        @DisplayName("TC-08: Submit REGISTER form successfully")
        void submitFeedback_Register_Success() throws Exception {
            // Arrange
            Long accountId = 1L;
            Long customerId = 100L;
            Account account = new Account();
            account.setAccountId(accountId);
            account.setEmail("test@example.com");
            Customer customer = new Customer();
            customer.setCustomerId(customerId);
            customer.setAccount(account);
            
            SubmitResponseRequest.ResponseItem responseItem = new SubmitResponseRequest.ResponseItem();
            responseItem.setQuestionId(1L);
            responseItem.setResponseValue("Test Answer");
            List<SubmitResponseRequest.ResponseItem> responses = List.of(responseItem);

            when(customerRepo.findByAccount_AccountId(accountId)).thenReturn(Optional.of(customer));
            when(eventFormService.getFormById(FORM_ID)).thenReturn(registerFormDTO);
            doNothing().when(eventFormService).submitResponse(any(SubmitResponseRequest.class));

            // Act & Assert
            mockMvc.perform(post("/forms/feedback/submit")
                            .param("formId", FORM_ID.toString())
                            .param("eventId", EVENT_ID.toString())
                            .param("responses[0].questionId", "1")
                            .param("responses[0].responseValue", "Test Answer")
                            .requestAttr("currentUserId", accountId))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("/events/" + EVENT_ID + "?success=form_submitted*"));

            verify(eventFormService).submitResponse(any(SubmitResponseRequest.class));
        }

        @Test
        @DisplayName("TC-09: Submit CHECKIN form successfully")
        void submitFeedback_Checkin_Success() throws Exception {
            // Arrange
            Long accountId = 1L;
            Long customerId = 100L;
            Account account = new Account();
            account.setAccountId(accountId);
            account.setEmail("test@example.com");
            Customer customer = new Customer();
            customer.setCustomerId(customerId);
            customer.setAccount(account);
            
            SubmitResponseRequest.ResponseItem responseItem = new SubmitResponseRequest.ResponseItem();
            responseItem.setQuestionId(1L);
            responseItem.setResponseValue("Test Answer");
            List<SubmitResponseRequest.ResponseItem> responses = List.of(responseItem);

            when(customerRepo.findByAccount_AccountId(accountId)).thenReturn(Optional.of(customer));
            when(eventFormService.getFormById(FORM_ID + 1)).thenReturn(checkinFormDTO);
            when(attendanceService.isAlreadyCheckedIn(eq(EVENT_ID), anyString())).thenReturn(false);
            when(attendanceService.checkIn(eq(EVENT_ID), any())).thenReturn(null);
            doNothing().when(eventFormService).submitResponse(any(SubmitResponseRequest.class));

            // Act & Assert
            mockMvc.perform(post("/forms/feedback/submit")
                            .param("formId", String.valueOf(FORM_ID + 1))
                            .param("eventId", EVENT_ID.toString())
                            .param("responses[0].questionId", "1")
                            .param("responses[0].responseValue", "Test Answer")
                            .requestAttr("currentUserId", accountId))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("/events/" + EVENT_ID + "?success=checkin_success*"));

            verify(eventFormService).submitResponse(any(SubmitResponseRequest.class));
            verify(attendanceService).checkIn(eq(EVENT_ID), any());
        }

        @Test
        @DisplayName("TC-10: Submit FEEDBACK form successfully")
        void submitFeedback_Feedback_Success() throws Exception {
            // Arrange
            Long accountId = 1L;
            Long customerId = 100L;
            Account account = new Account();
            account.setAccountId(accountId);
            account.setEmail("test@example.com");
            Customer customer = new Customer();
            customer.setCustomerId(customerId);
            customer.setAccount(account);
            
            SubmitResponseRequest.ResponseItem responseItem = new SubmitResponseRequest.ResponseItem();
            responseItem.setQuestionId(1L);
            responseItem.setResponseValue("Test Answer");
            List<SubmitResponseRequest.ResponseItem> responses = List.of(responseItem);

            when(customerRepo.findByAccount_AccountId(accountId)).thenReturn(Optional.of(customer));
            when(eventFormService.getFormById(FORM_ID + 2)).thenReturn(feedbackFormDTO);
            doNothing().when(eventFormService).submitResponse(any(SubmitResponseRequest.class));

            // Act & Assert - Controller gets email from SecurityContext, which is null when Security is excluded
            // So checkOut won't be called in this test scenario
            mockMvc.perform(post("/forms/feedback/submit")
                            .param("formId", String.valueOf(FORM_ID + 2))
                            .param("eventId", EVENT_ID.toString())
                            .param("responses[0].questionId", "1")
                            .param("responses[0].responseValue", "Test Answer")
                            .requestAttr("currentUserId", accountId))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("/events/" + EVENT_ID + "?success=feedback_submitted*"));

            verify(eventFormService).submitResponse(any(SubmitResponseRequest.class));
            // Note: checkOut is NOT called because SecurityContext.getAuthentication() returns null
            verify(attendanceService, never()).checkOut(anyLong(), anyString());
        }

        @Test
        @DisplayName("TC-11: Submit form fails when form ID is null")
        void submitFeedback_FormIdNull() throws Exception {
            // Act & Assert
            mockMvc.perform(post("/forms/feedback/submit")
                            .param("eventId", EVENT_ID.toString()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("/events/" + EVENT_ID + "?error=submission_failed*"));
        }

        @Test
        @DisplayName("TC-12: Submit form fails when user not logged in")
        void submitFeedback_CustomerIdNull() throws Exception {
            // Act & Assert - No currentUserId attribute means user not logged in
            mockMvc.perform(post("/forms/feedback/submit")
                            .param("formId", FORM_ID.toString())
                            .param("eventId", EVENT_ID.toString()))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("/events/" + EVENT_ID + "?error=submission_failed*"));
        }
    }

    @Nested
    @DisplayName("viewResponses Tests")
    class ViewResponsesTests {
        @Test
        @DisplayName("TC-13: View responses successfully with all form types")
        void viewResponses_Success() throws Exception {
            // Arrange
            when(eventFormService.getResponsesByEventIdAndFormType(EVENT_ID, EventForm.FormType.REGISTER))
                    .thenReturn(List.of(responseDTO));
            when(eventFormService.getResponsesByEventIdAndFormType(EVENT_ID, EventForm.FormType.CHECKIN))
                    .thenReturn(List.of());
            when(eventFormService.getResponsesByEventIdAndFormType(EVENT_ID, EventForm.FormType.FEEDBACK))
                    .thenReturn(List.of());

            // Act & Assert
            mockMvc.perform(get("/forms/responses/{eventId}", EVENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(view().name("host/view-responses"))
                    .andExpect(model().attributeExists("registerResponses"))
                    .andExpect(model().attributeExists("checkinResponses"))
                    .andExpect(model().attributeExists("feedbackResponses"))
                    .andExpect(model().attributeExists("eventId"));
        }

        @Test
        @DisplayName("TC-14: View responses handles service exception")
        void viewResponses_ServiceException() throws Exception {
            // Arrange
            when(eventFormService.getResponsesByEventIdAndFormType(EVENT_ID, EventForm.FormType.REGISTER))
                    .thenThrow(new RuntimeException("Database error"));

            // Act & Assert
            mockMvc.perform(get("/forms/responses/{eventId}", EVENT_ID))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("/music/" + EVENT_ID + "*error=*"));
        }
    }

    @Nested
    @DisplayName("showCreateForm Tests")
    class ShowCreateFormTests {
        @Test
        @DisplayName("TC-15: Show create form with REGISTER type")
        void showCreateForm_RegisterType() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/forms/create/{eventId}", EVENT_ID)
                            .param("type", "REGISTER"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("host/create-feedback-form"))
                    .andExpect(model().attributeExists("createFormRequest"))
                    .andExpect(model().attribute("pageTitle", "Create Register Form"));
        }

        @Test
        @DisplayName("TC-16: Show create form with CHECKIN type")
        void showCreateForm_CheckinType() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/forms/create/{eventId}", EVENT_ID)
                            .param("type", "CHECKIN"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("pageTitle", "Create Check-in Form"));
        }

        @Test
        @DisplayName("TC-17: Show create form with FEEDBACK type")
        void showCreateForm_FeedbackType() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/forms/create/{eventId}", EVENT_ID)
                            .param("type", "FEEDBACK"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("pageTitle", "Create Feedback Form"));
        }

        @Test
        @DisplayName("TC-18: Show create form without type (defaults to FEEDBACK)")
        void showCreateForm_NoType() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/forms/create/{eventId}", EVENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("pageTitle", "Create Feedback Form"));
        }

        @Test
        @DisplayName("TC-19: Show create form with invalid type")
        void showCreateForm_InvalidType() throws Exception {
            // Act & Assert
            mockMvc.perform(get("/forms/create/{eventId}", EVENT_ID)
                            .param("type", "INVALID"))
                    .andExpect(status().isOk())
                    .andExpect(model().attribute("pageTitle", "Create Feedback Form"));
        }
    }

    @Nested
    @DisplayName("createForm Tests")
    class CreateFormTests {
        @Test
        @DisplayName("TC-20: Create form successfully")
        void createForm_Success() throws Exception {
            // Arrange
            CreateFormRequest request = new CreateFormRequest();
            request.setEventId(EVENT_ID);
            request.setFormTitle("Test Form");
            request.setFormDescription("Test Description");
            request.setFormType(EventForm.FormType.REGISTER);

            when(eventFormService.createForm(any(CreateFormRequest.class))).thenReturn(registerFormDTO);

            // Act & Assert
            mockMvc.perform(post("/forms/create")
                            .param("eventId", EVENT_ID.toString())
                            .param("formTitle", "Test Form")
                            .param("formDescription", "Test Description")
                            .param("formType", "REGISTER"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("/manage/event/" + EVENT_ID + "/create-forms?success=form_created*"));


            verify(eventFormService).createForm(any(CreateFormRequest.class));
        }

        @Test
        @DisplayName("TC-21: Create form fails when event ID is null")
        void createForm_EventIdNull() throws Exception {

            mockMvc.perform(post("/forms/create")
                            .param("formTitle", "Test Form"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("/manage/event/0/create-forms?error=*"));
        }


        @Test
        @DisplayName("TC-22: Create form handles service exception")
        void createForm_ServiceException() throws Exception {
            // Arrange
            when(eventFormService.createForm(any(CreateFormRequest.class)))
                    .thenThrow(new RuntimeException("Form creation failed"));

            // Act + Assert
            mockMvc.perform(post("/forms/create")
                            .param("eventId", EVENT_ID.toString())
                            .param("formTitle", "Test Form"))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern(
                            "/manage/event/" + EVENT_ID + "/create-forms?error=*"
                    ));
        }

    }



        @Nested
    @DisplayName("API Endpoints Tests")
    class ApiEndpointsTests {
        @Test
        @DisplayName("TC-23: Get form by event ID API successfully")
        void getFormByEventId_Success() throws Exception {
            // Arrange
            when(eventFormService.getFormByEventId(EVENT_ID)).thenReturn(feedbackFormDTO);

            // Act & Assert
            mockMvc.perform(get("/forms/api/event/{eventId}", EVENT_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.formId").value(FORM_ID + 2))
                    .andExpect(jsonPath("$.formType").value("FEEDBACK"));
        }

        @Test
        @DisplayName("TC-24: Get form by event ID API returns 404 when not found")
        void getFormByEventId_NotFound() throws Exception {
            // Arrange
            when(eventFormService.getFormByEventId(EVENT_ID))
                    .thenThrow(new RuntimeException("Form not found"));

            // Act & Assert
            mockMvc.perform(get("/forms/api/event/{eventId}", EVENT_ID))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("TC-25: Submit response API successfully")
        void submitResponseApi_Success() throws Exception {
            // Arrange
            doNothing().when(eventFormService).submitResponse(any(SubmitResponseRequest.class));

            // Act & Assert
            mockMvc.perform(post("/forms/api/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"formId\":1,\"customerId\":1,\"responses\":[]}"))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Response submitted successfully"));

            verify(eventFormService).submitResponse(any(SubmitResponseRequest.class));
        }

        @Test
        @DisplayName("TC-26: Submit response API fails when exception occurs")
        void submitResponseApi_Failure() throws Exception {
            // Arrange
            doThrow(new RuntimeException("Submission failed"))
                    .when(eventFormService).submitResponse(any(SubmitResponseRequest.class));

            // Act & Assert
            mockMvc.perform(post("/forms/api/submit")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"formId\":1,\"customerId\":1,\"responses\":[]}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Failed to submit response")));
        }
    }
}


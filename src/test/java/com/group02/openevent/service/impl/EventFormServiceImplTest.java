package com.group02.openevent.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.group02.openevent.dto.form.*;
import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.form.EventForm;
import com.group02.openevent.model.form.FormQuestion;
import com.group02.openevent.model.form.FormResponse;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.model.user.User;
import com.group02.openevent.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EventFormServiceImpl Unit Tests")
class EventFormServiceImplTest {

    @InjectMocks
    private EventFormServiceImpl formService;

    @Mock
    private IEventFormRepo eventFormRepo;
    @Mock
    private IFormQuestionRepo formQuestionRepo;
    @Mock
    private IFormResponseRepo formResponseRepo;
    @Mock
    private IEventRepo eventRepo;
    @Mock
    private ICustomerRepo customerRepo;
    @Mock
    private ObjectMapper objectMapper;

    private Event event;
    private EventForm form;
    private Customer customer;
    private FormQuestion question;
    private FormResponse response;

    private static final Long EVENT_ID = 1L;
    private static final Long FORM_ID = 10L;
    private static final Long QUESTION_ID = 20L;
    private static final Long CUSTOMER_ID = 30L;
    private static final Long RESPONSE_ID = 40L;

    @BeforeEach
    void setUp() {
        event = new Event();
        event.setId(EVENT_ID);
        event.setTitle("Test Event");

        form = new EventForm();
        form.setFormId(FORM_ID);
        form.setEvent(event);
        form.setFormTitle("Test Form");
        form.setFormDescription("Test Description");
        form.setFormType(EventForm.FormType.FEEDBACK);
        form.setIsActive(true);

        Account account = new Account();
        account.setAccountId(1L);
        account.setEmail("test@example.com");
        User user = new User();
        user.setAccount(account);
        user.setUserId(1L);
        user.setName("Test User");
        customer = new Customer();
        customer.setCustomerId(CUSTOMER_ID);
        customer.setUser(user);

        question = new FormQuestion();
        question.setQuestionId(QUESTION_ID);
        question.setEventForm(form);
        question.setQuestionText("Test Question");
        question.setQuestionType(com.group02.openevent.model.form.FormQuestion.QuestionType.TEXT);
        question.setIsRequired(true);

        response = new FormResponse();
        response.setResponseId(RESPONSE_ID);
        response.setEventForm(form);
        response.setFormQuestion(question);
        response.setCustomer(customer);
        response.setResponseValue("Test Answer");
        response.setSubmittedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("createForm Tests")
    class CreateFormTests {
        @Test
        @DisplayName("TC-01: Create form successfully (REGISTER type)")
        void createForm_Register_Success() throws Exception {
            // Arrange
            CreateFormRequest request = new CreateFormRequest();
            request.setEventId(EVENT_ID);
            request.setFormTitle("Register Form");
            request.setFormDescription("Register for event");
            request.setFormType(EventForm.FormType.REGISTER);

            CreateQuestionRequest questionRequest = new CreateQuestionRequest();
            questionRequest.setQuestionText("Full Name");
            questionRequest.setQuestionType(com.group02.openevent.model.form.FormQuestion.QuestionType.TEXT);
            questionRequest.setIsRequired(true);
            questionRequest.setQuestionOrder(1);
            request.setQuestions(List.of(questionRequest));

            when(eventRepo.findById(EVENT_ID)).thenReturn(Optional.of(event));
            when(eventFormRepo.findActiveByEventIdAndType(EVENT_ID, EventForm.FormType.REGISTER))
                    .thenReturn(Optional.empty());
            when(eventFormRepo.save(any(EventForm.class))).thenAnswer(invocation -> {
                EventForm saved = invocation.getArgument(0);
                saved.setFormId(FORM_ID);
                return saved;
            });
            when(formQuestionRepo.save(any(FormQuestion.class))).thenAnswer(invocation -> {
                FormQuestion saved = invocation.getArgument(0);
                saved.setQuestionId(QUESTION_ID);
                return saved;
            });

            // Act
            EventFormDTO result = formService.createForm(request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getFormId()).isEqualTo(FORM_ID);
            assertThat(result.getFormType()).isEqualTo(EventForm.FormType.REGISTER);
            verify(eventFormRepo).save(any(EventForm.class));
            verify(formQuestionRepo).save(any(FormQuestion.class));
        }

        @Test
        @DisplayName("TC-02: Create form successfully (CHECKIN type)")
        void createForm_Checkin_Success() throws Exception {
            // Arrange
            CreateFormRequest request = new CreateFormRequest();
            request.setEventId(EVENT_ID);
            request.setFormTitle("Check-in Form");
            request.setFormDescription("Check-in for event");
            request.setFormType(EventForm.FormType.CHECKIN);
            request.setQuestions(List.of());

            when(eventRepo.findById(EVENT_ID)).thenReturn(Optional.of(event));
            when(eventFormRepo.findActiveByEventIdAndType(EVENT_ID, EventForm.FormType.CHECKIN))
                    .thenReturn(Optional.empty());
            when(eventFormRepo.save(any(EventForm.class))).thenAnswer(invocation -> {
                EventForm saved = invocation.getArgument(0);
                saved.setFormId(FORM_ID);
                return saved;
            });

            // Act
            EventFormDTO result = formService.createForm(request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getFormType()).isEqualTo(EventForm.FormType.CHECKIN);
        }

        @Test
        @DisplayName("TC-03: Create form successfully (FEEDBACK type)")
        void createForm_Feedback_Success() throws Exception {
            // Arrange
            CreateFormRequest request = new CreateFormRequest();
            request.setEventId(EVENT_ID);
            request.setFormTitle("Feedback Form");
            request.setFormDescription("Feedback for event");
            request.setFormType(EventForm.FormType.FEEDBACK);
            request.setQuestions(List.of());

            when(eventRepo.findById(EVENT_ID)).thenReturn(Optional.of(event));
            when(eventFormRepo.findActiveByEventIdAndType(EVENT_ID, EventForm.FormType.FEEDBACK))
                    .thenReturn(Optional.empty());
            when(eventFormRepo.save(any(EventForm.class))).thenAnswer(invocation -> {
                EventForm saved = invocation.getArgument(0);
                saved.setFormId(FORM_ID);
                return saved;
            });

            // Act
            EventFormDTO result = formService.createForm(request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getFormType()).isEqualTo(EventForm.FormType.FEEDBACK);
        }

        @Test
        @DisplayName("TC-04: Create form fails when event not found")
        void createForm_EventNotFound() {
            // Arrange
            CreateFormRequest request = new CreateFormRequest();
            request.setEventId(999L);
            request.setFormType(EventForm.FormType.REGISTER);

            when(eventRepo.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> formService.createForm(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Event not found");
        }

        @Test
        @DisplayName("TC-05: Create form fails when form type is null")
        void createForm_FormTypeNull() {
            // Arrange
            CreateFormRequest request = new CreateFormRequest();
            request.setEventId(EVENT_ID);
            request.setFormType(null);

            when(eventRepo.findById(EVENT_ID)).thenReturn(Optional.of(event));

            // Act & Assert
            assertThatThrownBy(() -> formService.createForm(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Form type is required");
        }

        @Test
        @DisplayName("TC-06: Create form updates existing form (upsert)")
        void createForm_UpsertExisting() throws Exception {
            // Arrange
            CreateFormRequest request = new CreateFormRequest();
            request.setEventId(EVENT_ID);
            request.setFormTitle("Updated Form");
            request.setFormDescription("Updated Description");
            request.setFormType(EventForm.FormType.REGISTER);
            request.setQuestions(List.of());

            when(eventRepo.findById(EVENT_ID)).thenReturn(Optional.of(event));
            when(eventFormRepo.findActiveByEventIdAndType(EVENT_ID, EventForm.FormType.REGISTER))
                    .thenReturn(Optional.of(form));
            when(eventFormRepo.save(any(EventForm.class))).thenReturn(form);

            // Act
            EventFormDTO result = formService.createForm(request);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getFormId()).isEqualTo(FORM_ID);
            verify(eventFormRepo).save(form);
        }
    }

    @Nested
    @DisplayName("getActiveFormByEventIdAndType Tests")
    class GetActiveFormByEventIdAndTypeTests {
        @Test
        @DisplayName("TC-07: Get active form by event ID and type (REGISTER)")
        void getActiveFormByEventIdAndType_Register_Success() {
            // Arrange
            form.setFormType(EventForm.FormType.REGISTER);
            when(eventFormRepo.findActiveByEventIdAndType(EVENT_ID, EventForm.FormType.REGISTER))
                    .thenReturn(Optional.of(form));

            // Act
            EventFormDTO result = formService.getActiveFormByEventIdAndType(EVENT_ID, EventForm.FormType.REGISTER);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getFormType()).isEqualTo(EventForm.FormType.REGISTER);
        }

        @Test
        @DisplayName("TC-08: Get active form by event ID and type (CHECKIN)")
        void getActiveFormByEventIdAndType_Checkin_Success() {
            // Arrange
            form.setFormType(EventForm.FormType.CHECKIN);
            when(eventFormRepo.findActiveByEventIdAndType(EVENT_ID, EventForm.FormType.CHECKIN))
                    .thenReturn(Optional.of(form));

            // Act
            EventFormDTO result = formService.getActiveFormByEventIdAndType(EVENT_ID, EventForm.FormType.CHECKIN);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getFormType()).isEqualTo(EventForm.FormType.CHECKIN);
        }

        @Test
        @DisplayName("TC-09: Get active form fails when form not found")
        void getActiveFormByEventIdAndType_NotFound() {
            // Arrange
            when(eventFormRepo.findActiveByEventIdAndType(EVENT_ID, EventForm.FormType.REGISTER))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> formService.getActiveFormByEventIdAndType(EVENT_ID, EventForm.FormType.REGISTER))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("No active REGISTER form found");
        }
    }

    @Nested
    @DisplayName("submitResponse Tests")
    class SubmitResponseTests {
        @Test
        @DisplayName("TC-10: Submit response for REGISTER form successfully")
        void submitResponse_Register_Success() {
            // Arrange
            SubmitResponseRequest request = new SubmitResponseRequest();
            request.setFormId(FORM_ID);
            request.setCustomerId(CUSTOMER_ID);

            SubmitResponseRequest.ResponseItem responseItem = new SubmitResponseRequest.ResponseItem();
            responseItem.setQuestionId(QUESTION_ID);
            responseItem.setResponseValue("Answer");
            request.setResponses(List.of(responseItem));

            form.setFormType(EventForm.FormType.REGISTER);

            when(eventFormRepo.findById(FORM_ID)).thenReturn(Optional.of(form));
            when(customerRepo.getReferenceById(CUSTOMER_ID)).thenReturn(customer);
            when(formQuestionRepo.findById(QUESTION_ID)).thenReturn(Optional.of(question));
            when(formResponseRepo.save(any(FormResponse.class))).thenAnswer(invocation -> {
                FormResponse saved = invocation.getArgument(0);
                saved.setResponseId(RESPONSE_ID);
                return saved;
            });

            // Act
            formService.submitResponse(request);

            // Assert
            verify(formResponseRepo).save(any(FormResponse.class));
        }

        @Test
        @DisplayName("TC-11: Submit response fails when form not found")
        void submitResponse_FormNotFound() {
            // Arrange
            SubmitResponseRequest request = new SubmitResponseRequest();
            request.setFormId(999L);
            request.setCustomerId(CUSTOMER_ID);

            when(eventFormRepo.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> formService.submitResponse(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Form not found");
        }

        @Test
        @DisplayName("TC-12: Submit response fails when question not found")
        void submitResponse_QuestionNotFound() {
            // Arrange
            SubmitResponseRequest request = new SubmitResponseRequest();
            request.setFormId(FORM_ID);
            request.setCustomerId(CUSTOMER_ID);

            SubmitResponseRequest.ResponseItem responseItem = new SubmitResponseRequest.ResponseItem();
            responseItem.setQuestionId(999L);
            responseItem.setResponseValue("Answer");
            request.setResponses(List.of(responseItem));

            when(eventFormRepo.findById(FORM_ID)).thenReturn(Optional.of(form));
            when(customerRepo.getReferenceById(CUSTOMER_ID)).thenReturn(customer);
            when(formQuestionRepo.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> formService.submitResponse(request))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Question not found");
        }
    }

    @Nested
    @DisplayName("getResponsesByEventIdAndFormType Tests")
    class GetResponsesByEventIdAndFormTypeTests {
        @Test
        @DisplayName("TC-13: Get responses by event ID and form type (REGISTER)")
        void getResponsesByEventIdAndFormType_Register_Success() {
            // Arrange
            response.setEventForm(form);
            form.setFormType(EventForm.FormType.REGISTER);

            when(formResponseRepo.findByEventIdAndFormType(EVENT_ID, EventForm.FormType.REGISTER))
                    .thenReturn(List.of(response));

            // Act
            List<FormResponseDTO> result = formService.getResponsesByEventIdAndFormType(
                    EVENT_ID, EventForm.FormType.REGISTER);

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getFormType()).isEqualTo(EventForm.FormType.REGISTER);
            verify(formResponseRepo).findByEventIdAndFormType(EVENT_ID, EventForm.FormType.REGISTER);
        }

        @Test
        @DisplayName("TC-14: Get responses by event ID and form type (CHECKIN)")
        void getResponsesByEventIdAndFormType_Checkin_Success() {
            // Arrange
            form.setFormType(EventForm.FormType.CHECKIN);
            response.setEventForm(form);

            when(formResponseRepo.findByEventIdAndFormType(EVENT_ID, EventForm.FormType.CHECKIN))
                    .thenReturn(List.of(response));

            // Act
            List<FormResponseDTO> result = formService.getResponsesByEventIdAndFormType(
                    EVENT_ID, EventForm.FormType.CHECKIN);

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getFormType()).isEqualTo(EventForm.FormType.CHECKIN);
        }

        @Test
        @DisplayName("TC-15: Get responses by event ID and form type (FEEDBACK)")
        void getResponsesByEventIdAndFormType_Feedback_Success() {
            // Arrange
            form.setFormType(EventForm.FormType.FEEDBACK);
            response.setEventForm(form);

            when(formResponseRepo.findByEventIdAndFormType(EVENT_ID, EventForm.FormType.FEEDBACK))
                    .thenReturn(List.of(response));

            // Act
            List<FormResponseDTO> result = formService.getResponsesByEventIdAndFormType(
                    EVENT_ID, EventForm.FormType.FEEDBACK);

            // Assert
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getFormType()).isEqualTo(EventForm.FormType.FEEDBACK);
        }

        @Test
        @DisplayName("TC-16: Get responses returns empty list when no responses")
        void getResponsesByEventIdAndFormType_Empty() {
            // Arrange
            when(formResponseRepo.findByEventIdAndFormType(EVENT_ID, EventForm.FormType.REGISTER))
                    .thenReturn(List.of());

            // Act
            List<FormResponseDTO> result = formService.getResponsesByEventIdAndFormType(
                    EVENT_ID, EventForm.FormType.REGISTER);

            // Assert
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("getFormByEventId Tests")
    class GetFormByEventIdTests {
        @Test
        @DisplayName("TC-17: Get form by event ID successfully")
        void getFormByEventId_Success() {
            // Arrange
            when(eventFormRepo.findByEventIdAndActive(EVENT_ID)).thenReturn(Optional.of(form));

            // Act
            EventFormDTO result = formService.getFormByEventId(EVENT_ID);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getFormId()).isEqualTo(FORM_ID);
        }

        @Test
        @DisplayName("TC-18: Get form by event ID fails when not found")
        void getFormByEventId_NotFound() {
            // Arrange
            when(eventFormRepo.findByEventIdAndActive(EVENT_ID)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> formService.getFormByEventId(EVENT_ID))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("No active form found");
        }
    }

    @Nested
    @DisplayName("getFormById Tests")
    class GetFormByIdTests {
        @Test
        @DisplayName("TC-19: Get form by ID successfully")
        void getFormById_Success() {
            // Arrange
            when(eventFormRepo.findById(FORM_ID)).thenReturn(Optional.of(form));

            // Act
            EventFormDTO result = formService.getFormById(FORM_ID);

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getFormId()).isEqualTo(FORM_ID);
        }

        @Test
        @DisplayName("TC-20: Get form by ID fails when not found")
        void getFormById_NotFound() {
            // Arrange
            when(eventFormRepo.findById(999L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> formService.getFormById(999L))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Form not found");
        }
    }
}


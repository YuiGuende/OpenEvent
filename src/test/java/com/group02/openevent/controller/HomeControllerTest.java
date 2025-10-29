package com.group02.openevent.controller;

import com.group02.openevent.dto.home.EventCardDTO;
import com.group02.openevent.dto.user.UserOrderDTO;
import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.repository.IAccountRepo;
import com.group02.openevent.repository.ICustomerRepo;
import com.group02.openevent.repository.IEventRepo;
import com.group02.openevent.repository.IOrderRepo;
import com.group02.openevent.service.EventService;
import com.group02.openevent.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("HomeController Tests")
class HomeControllerTest {

    @Mock private IAccountRepo accountRepo;
    @Mock private ICustomerRepo customerRepo;
    @Mock private IEventRepo eventRepo;
    @Mock private IOrderRepo orderRepo;
    @Mock private EventService eventService;
    @Mock private OrderService orderService;

    @InjectMocks
    private HomeController homeController;

    private MockMvc mockMvc;

    @BeforeEach
    void setup() {
        // Ensure field-injected dependencies are set explicitly
        ReflectionTestUtils.setField(homeController, "eventService", eventService);
        ReflectionTestUtils.setField(homeController, "orderService", orderService);
        mockMvc = MockMvcBuilders.standaloneSetup(homeController).build();
    }

    @Nested
    @DisplayName("GET /")
    class GetHomeTests {

        @Test
        @DisplayName("When not logged in, returns index with empty myEvents")
        void home_whenNoUser_returnsIndexWithEmptyMyEvents() throws Exception {
            when(eventService.getPosterEvents()).thenReturn(List.of());
            when(eventService.getLiveEvents(6)).thenReturn(List.of());
            when(eventService.getRecentEvents(3)).thenReturn(List.of());
            when(eventService.getRecommendedEvents(6)).thenReturn(List.of());

            mockMvc.perform(get("/"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("index"))
                    .andExpect(model().attributeExists("posterEvents"))
                    .andExpect(model().attributeExists("liveEvents"))
                    .andExpect(model().attributeExists("latestEvents"))
                    .andExpect(model().attributeExists("myEvents"))
                    .andExpect(model().attributeExists("recommendedEvents"))
                    .andExpect(model().attribute("myEvents", List.of()));
        }

        @Test
        @DisplayName("When logged in, builds myEvents from orders and events")
        void home_whenLoggedIn_populatesMyEvents() throws Exception {
            MockHttpSession session = new MockHttpSession();
            session.setAttribute("ACCOUNT_ID", 1L);

            Customer customer = new Customer();
            when(customerRepo.findByAccount_AccountId(1L)).thenReturn(Optional.of(customer));

            UserOrderDTO o1 = new UserOrderDTO();
            o1.setEventId(10L);
            UserOrderDTO o2 = new UserOrderDTO();
            o2.setEventId(20L);
            UserOrderDTO o3 = new UserOrderDTO();
            o3.setEventId(20L); // duplicate to test distinct
            when(orderService.getOrderDTOsByCustomer(eq(customer), isNull())).thenReturn(List.of(o1, o2, o3));

            Event e1 = new Event();
            Event e2 = new Event();
            when(eventRepo.findAllById(any()))
                    .thenReturn(List.of(e1, e2));

            EventCardDTO d1 = new EventCardDTO();
            EventCardDTO d2 = new EventCardDTO();
            when(eventService.convertToDTO(e1)).thenReturn(d1);
            when(eventService.convertToDTO(e2)).thenReturn(d2);

            when(eventService.getPosterEvents()).thenReturn(List.of());
            when(eventService.getLiveEvents(6)).thenReturn(List.of());
            when(eventService.getRecentEvents(3)).thenReturn(List.of());
            when(eventService.getRecommendedEvents(6)).thenReturn(List.of());

            mockMvc.perform(get("/").session(session))
                    .andExpect(status().isOk())
                    .andExpect(view().name("index"))
                    .andExpect(model().attributeExists("myEvents"))
                    .andExpect(model().attribute("myEvents", List.of(d1, d2)));
        }

        @Test
        @DisplayName("When logged in but no Customer found, myEvents is empty")
        void home_whenLoggedIn_noCustomer_myEventsEmpty() throws Exception {
            MockHttpSession session = new MockHttpSession();
            session.setAttribute("ACCOUNT_ID", 1L);

            when(customerRepo.findByAccount_AccountId(1L)).thenReturn(Optional.empty());

            when(eventService.getPosterEvents()).thenReturn(List.of());
            when(eventService.getLiveEvents(6)).thenReturn(List.of());
            when(eventService.getRecentEvents(3)).thenReturn(List.of());
            when(eventService.getRecommendedEvents(6)).thenReturn(List.of());

            mockMvc.perform(get("/").session(session))
                    .andExpect(status().isOk())
                    .andExpect(view().name("index"))
                    .andExpect(model().attribute("myEvents", List.of()));
        }

        @Test
        @DisplayName("When logged in with Customer but no orders, myEvents is empty")
        void home_whenLoggedIn_noOrders_myEventsEmpty() throws Exception {
            MockHttpSession session = new MockHttpSession();
            session.setAttribute("ACCOUNT_ID", 1L);

            Customer customer = new Customer();
            when(customerRepo.findByAccount_AccountId(1L)).thenReturn(Optional.of(customer));
            when(orderService.getOrderDTOsByCustomer(eq(customer), isNull())).thenReturn(List.of());

            when(eventService.getPosterEvents()).thenReturn(List.of());
            when(eventService.getLiveEvents(6)).thenReturn(List.of());
            when(eventService.getRecentEvents(3)).thenReturn(List.of());
            when(eventService.getRecommendedEvents(6)).thenReturn(List.of());

            mockMvc.perform(get("/").session(session))
                    .andExpect(status().isOk())
                    .andExpect(view().name("index"))
                    .andExpect(model().attribute("myEvents", List.of()));

            verify(eventRepo, never()).findAllById(any());
        }

        @Test
        @DisplayName("Maps latestEvents using convertToDTO")
        void home_mapsLatestEvents_usingConvertToDTO() throws Exception {
            Event e1 = new Event();
            Event e2 = new Event();
            EventCardDTO d1 = new EventCardDTO();
            EventCardDTO d2 = new EventCardDTO();

            when(eventService.getPosterEvents()).thenReturn(List.of());
            when(eventService.getLiveEvents(6)).thenReturn(List.of());
            when(eventService.getRecentEvents(3)).thenReturn(List.of(e1, e2));
            when(eventService.convertToDTO(e1)).thenReturn(d1);
            when(eventService.convertToDTO(e2)).thenReturn(d2);
            when(eventService.getRecommendedEvents(6)).thenReturn(List.of());

            mockMvc.perform(get("/"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("index"))
                    .andExpect(model().attribute("latestEvents", List.of(d1, d2)))
                    .andExpect(model().attribute("myEvents", List.of()));
        }

        @Test
        @DisplayName("When service throws, returns index with empty lists")
        void home_whenException_returnsIndexWithEmptyLists() throws Exception {
            when(eventService.getPosterEvents()).thenThrow(new RuntimeException("boom"));

            mockMvc.perform(get("/"))
                    .andExpect(status().isOk())
                    .andExpect(view().name("index"))
                    .andExpect(model().attribute("posterEvents", List.of()))
                    .andExpect(model().attribute("liveEvents", List.of()))
                    .andExpect(model().attribute("myEvents", List.of()))
                    .andExpect(model().attribute("recommendedEvents", List.of()));
        }
    }

    @Nested
    @DisplayName("GET /api/current-user")
    class GetCurrentUserTests {

        @Test
        @DisplayName("When not logged in, authenticated=false")
        void currentUser_whenNotLoggedIn_returnsFalse() throws Exception {
            mockMvc.perform(get("/api/current-user"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.authenticated").value(false));
        }

        @Test
        @DisplayName("When account not found, authenticated=false")
        void currentUser_whenAccountMissing_returnsFalse() throws Exception {
            MockHttpSession session = new MockHttpSession();
            session.setAttribute("ACCOUNT_ID", 1L);
            when(accountRepo.findById(1L)).thenReturn(Optional.empty());

            mockMvc.perform(get("/api/current-user").session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.authenticated").value(false));
        }

        @Test
        @DisplayName("When account exists, authenticated=true with info")
        void currentUser_whenAccountExists_returnsInfo() throws Exception {
            MockHttpSession session = new MockHttpSession();
            session.setAttribute("ACCOUNT_ID", 2L);
            Account acc = new Account();
            acc.setAccountId(2L);
            acc.setEmail("user@example.com");
            acc.setRole(com.group02.openevent.model.enums.Role.CUSTOMER);
            // role is enum; to avoid NPE, set a dummy via reflection or assume non-null? Using default may NPE.
            // Easiest: stub repository and then relax assertion to only check authenticated=true and accountId/email presence if available.
            when(accountRepo.findById(2L)).thenReturn(Optional.of(acc));

            mockMvc.perform(get("/api/current-user").session(session))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.authenticated").value(true))
                    .andExpect(jsonPath("$.accountId").value(2))
                    .andExpect(jsonPath("$.email").value("user@example.com"));
        }
    }

    @Nested
    @DisplayName("POST /api/logout")
    class LogoutTests {

        @Test
        @DisplayName("Invalidates session and returns OK")
        void logout_invalidatesSession_returnsOk() throws Exception {
            MockHttpSession session = new MockHttpSession();
            session.setAttribute("ACCOUNT_ID", 123L);

            mockMvc.perform(post("/api/logout").session(session))
                    .andExpect(status().isOk())
                    .andExpect(content().string("Logged out successfully"));

            assertThrows(IllegalStateException.class, () -> session.getAttribute("ACCOUNT_ID"));
        }
    }
}



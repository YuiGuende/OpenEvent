package com.group02.openevent.ai.service;

import com.group02.openevent.ai.dto.PendingOrder;
import com.group02.openevent.dto.order.CreateOrderWithTicketTypeRequest;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.order.Order;
import com.group02.openevent.model.payment.Payment;
import com.group02.openevent.model.ticket.TicketType;
import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.repository.IUserRepo;
import com.group02.openevent.service.EventService;
import com.group02.openevent.service.OrderService;
import com.group02.openevent.service.PaymentService;
import com.group02.openevent.service.TicketTypeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class OrderAIServiceTest {

	@Mock private EventService eventService;
	@Mock private TicketTypeService ticketTypeService;
	@Mock private OrderService orderService;
	@Mock private PaymentService paymentService;
	@Mock private IUserRepo userRepo;
	@Mock private AgentEventService agentEventService;

	@InjectMocks
	private OrderAIService sut;

	private final long userId = 100L;
	private PendingOrder seedPending;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);

		seedPending = new PendingOrder();
		Event ev = new Event(); ev.setId(11L); ev.setTitle("Demo");
		TicketType tt = new TicketType(); tt.setTicketTypeId(22L); tt.setName("VIP"); tt.setPrice(BigDecimal.valueOf(500000));
		seedPending.setEvent(ev);
		seedPending.setTicketType(tt);
		seedPending.setParticipantName("Nguyen Van A");
		seedPending.setParticipantEmail("a@b.com");
		seedPending.setParticipantPhone("0123");
		seedPending.setCurrentStep(PendingOrder.OrderStep.CONFIRM_ORDER);
		TestPendingOrders.put(sut, userId, seedPending);
	}

	@ParameterizedTest(name = "incomplete: {0}")
	@MethodSource("incompleteFields")
	void confirmOrder_incomplete_returnsError(String caseName, Consumer<PendingOrder> mutator) {
		TestPendingOrders.edit(sut, userId, mutator);
		Map<String, Object> res = sut.confirmOrder(userId);
		assertThat(res.get("success")).isEqualTo(false);
		assertThat(String.valueOf(res.get("message"))).contains("Thông tin đơn hàng không đầy đủ");
	}

	static Stream<Arguments> incompleteFields() {
		return Stream.of(
			Arguments.of("missingEmail", (Consumer<PendingOrder>) po -> po.setParticipantEmail(null)),
			Arguments.of("missingName", (Consumer<PendingOrder>) po -> po.setParticipantName(null)),
			Arguments.of("missingTicket", (Consumer<PendingOrder>) po -> po.setTicketType(null)),
			Arguments.of("missingEvent", (Consumer<PendingOrder>) po -> po.setEvent(null))
		);
	}

	@ParameterizedTest(name = "agentReminderThrows={0}")
	@ValueSource(booleans = {true, false})
	void confirmOrder_success_variants(boolean reminderThrows) {
		Customer c = customer();
		when(userRepo.findByAccount_AccountId(userId)).thenReturn(Optional.of(c));
		Order order = new Order(); order.setOrderId(7L); order.setEvent(seedPending.getEvent());
		when(orderService.createOrderWithTicketTypes(ArgumentMatchers.any(CreateOrderWithTicketTypeRequest.class), eq(c))).thenReturn(order);
		Payment p = new Payment(); p.setPaymentId(3L); p.setCheckoutUrl("http://pay/7"); p.setQrCode("QR"); p.setAmount(BigDecimal.valueOf(500000));
		when(paymentService.createPaymentLinkForOrder(eq(order), anyString(), anyString())).thenReturn(p);
		if (reminderThrows) {
			doThrow(new RuntimeException("mail")).when(agentEventService).createOrUpdateEmailReminder(anyLong(), anyInt(), anyLong());
		}
		Map<String, Object> res = sut.confirmOrder(userId);
		assertThat(res.get("success")).isEqualTo(true);
		assertThat(res.get("orderId")).isEqualTo(order.getOrderId());
		assertThat(res.get("paymentUrl")).isEqualTo(p.getCheckoutUrl());
	}

	@ParameterizedTest(name = "exception: {0}")
	@ValueSource(strings = {"boom", "db-error", "payment-failed"})
	void confirmOrder_exception_bubblesMessage(String msg) {
		when(userRepo.findByAccount_AccountId(userId)).thenReturn(Optional.of(customer()));
		when(orderService.createOrderWithTicketTypes(any(), any())).thenThrow(new RuntimeException(msg));
		Map<String, Object> res = sut.confirmOrder(userId);
		assertThat(res.get("success")).isEqualTo(false);
		assertThat(String.valueOf(res.get("message"))).contains(msg);
	}

	private Customer customer() {
		Customer c = new Customer();
		Account a = new Account(); a.setAccountId(userId); a.setEmail("cust@example.com");
		c.setAccount(a); c.setCustomerId(9L);
		return c;
	}

	static class TestPendingOrders {
		@SuppressWarnings("unchecked")
		static void put(OrderAIService s, long uid, PendingOrder po) {
			try {
				var f = OrderAIService.class.getDeclaredField("pendingOrders");
				f.setAccessible(true);
				((Map<Long, PendingOrder>) f.get(s)).put(uid, po);
			} catch (Exception e) { throw new RuntimeException(e); }
		}
		static void edit(OrderAIService s, long uid, Consumer<PendingOrder> editor) {
			try {
				var f = OrderAIService.class.getDeclaredField("pendingOrders");
				f.setAccessible(true);
				Map<Long, PendingOrder> map = (Map<Long, PendingOrder>) f.get(s);
				PendingOrder po = map.get(uid);
				if (po == null) { po = new PendingOrder(); map.put(uid, po); }
				editor.accept(po);
			} catch (Exception e) { throw new RuntimeException(e); }
		}
	}
}









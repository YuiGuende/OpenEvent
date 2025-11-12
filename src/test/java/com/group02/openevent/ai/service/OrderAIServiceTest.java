package com.group02.openevent.ai.service;

import com.group02.openevent.ai.dto.PendingOrder;
import com.group02.openevent.dto.order.CreateOrderWithTicketTypeRequest;
import com.group02.openevent.model.event.Event;
import com.group02.openevent.model.order.Order;
import com.group02.openevent.model.payment.Payment;
import com.group02.openevent.model.ticket.TicketType;
import com.group02.openevent.model.account.Account;
import com.group02.openevent.model.user.Customer;
import com.group02.openevent.model.user.User;
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

	// ==== startOrderCreation ====
	@ParameterizedTest
	@DisplayName("startOrderCreation: no event found or no tickets")
	@MethodSource("startOrder_emptyCases")
	void startOrderCreation_emptyCases(java.util.List<Event> events, java.util.List<TicketType> tickets, String expectedContain) {
		when(eventService.findByTitleAndPublicStatus("Demo"))
				.thenReturn(events);
		if (events != null && !events.isEmpty()) {
			when(ticketTypeService.getTicketTypesByEventId(events.get(0).getId())).thenReturn(tickets);
		}
		String res = sut.startOrderCreation(userId, "Demo");
		assertThat(res).contains(expectedContain);
	}

	static Stream<Arguments> startOrder_emptyCases() {
		Event ev = new Event(); ev.setId(1L); ev.setTitle("E1");
		return Stream.of(
				Arguments.of(java.util.List.of(), java.util.List.of(), "Không tìm thấy sự kiện"),
				Arguments.of(java.util.List.of(ev), java.util.List.of(), "hiện không có vé nào")
		);
	}

	@org.junit.jupiter.api.Test
	void startOrderCreation_success_setsPendingAndListsTickets() {
		Event ev = new Event(); ev.setId(2L); ev.setTitle("Music Night");
		TicketType vip = new TicketType(); vip.setName("VIP"); vip.setPrice(BigDecimal.valueOf(100000)); vip.setTotalQuantity(10); vip.setSoldQuantity(0);
		TicketType std = new TicketType(); std.setName("Standard"); std.setPrice(BigDecimal.valueOf(50000)); std.setTotalQuantity(5); std.setSoldQuantity(0);
		when(eventService.findByTitleAndPublicStatus("Music"))
				.thenReturn(java.util.List.of(ev));
		when(ticketTypeService.getTicketTypesByEventId(2L)).thenReturn(java.util.List.of(vip, std));
		String res = sut.startOrderCreation(userId, "Music");
		assertThat(res).contains("Các loại vé có sẵn").contains("VIP").contains("Standard");
		assertThat(sut.hasPendingOrder(userId)).isTrue();
		assertThat(sut.getPendingOrder(userId).getEvent().getId()).isEqualTo(2L);
	}

	// ==== selectTicketType ====
	@org.junit.jupiter.api.Test
	void selectTicketType_requiresStart() {
		String msg = sut.selectTicketType(userId + 999, "VIP");
		assertThat(msg).contains("Vui lòng chọn sự kiện trước");
	}

	@org.junit.jupiter.api.Test
	void selectTicketType_notFound_listsOptions() {
		Event ev = new Event(); ev.setId(3L); ev.setTitle("Conf");
		TestPendingOrders.edit(sut, userId, po -> { po.setEvent(ev); });
		TicketType a = new TicketType(); a.setName("A"); a.setTotalQuantity(1); a.setSoldQuantity(0);
		TicketType b = new TicketType(); b.setName("B"); b.setTotalQuantity(1); b.setSoldQuantity(0);
		when(ticketTypeService.getTicketTypesByEventId(3L)).thenReturn(java.util.List.of(a,b));
		String msg = sut.selectTicketType(userId, "VIP");
		assertThat(msg).contains("Không tìm thấy loại vé").contains("A").contains("B");
	}

	@org.junit.jupiter.api.Test
	void selectTicketType_unavailable() {
		Event ev = new Event(); ev.setId(4L); ev.setTitle("Show");
		TestPendingOrders.edit(sut, userId, po -> { po.setEvent(ev); });
		TicketType vip = new TicketType(); vip.setName("VIP"); vip.setTotalQuantity(0); vip.setSoldQuantity(0);
		when(ticketTypeService.getTicketTypesByEventId(4L)).thenReturn(java.util.List.of(vip));
		String msg = sut.selectTicketType(userId, "vip");
		assertThat(msg).contains("đã hết");
	}

	@org.junit.jupiter.api.Test
	void selectTicketType_success_setsStepAndTicket() {
		Event ev = new Event(); ev.setId(5L); ev.setTitle("Meetup");
		TestPendingOrders.edit(sut, userId, po -> { po.setEvent(ev); });
		TicketType std = new TicketType(); std.setName("Standard"); std.setTotalQuantity(2); std.setSoldQuantity(0); std.setPrice(BigDecimal.valueOf(1000));
		when(ticketTypeService.getTicketTypesByEventId(5L)).thenReturn(java.util.List.of(std));
		String msg = sut.selectTicketType(userId, "stand");
		assertThat(msg).contains("Vui lòng cung cấp thông tin");
		assertThat(sut.getPendingOrder(userId).getTicketType().getName()).isEqualTo("Standard");
		assertThat(sut.getPendingOrder(userId).getCurrentStep()).isEqualTo(PendingOrder.OrderStep.PROVIDE_INFO);
	}

	// ==== provideInfo ====
	@org.junit.jupiter.api.Test
	void provideInfo_requiresTicket() {
		TestPendingOrders.edit(sut, userId, po -> { po.setTicketType(null); });
		String res = sut.provideInfo(userId, java.util.Map.of("name","A"));
		assertThat(res).contains("Vui lòng chọn loại vé trước");
	}

	@org.junit.jupiter.api.Test
	void provideInfo_incomplete_returnsMissing() {
		Event ev = new Event(); ev.setId(6L); ev.setTitle("Expo");
		TicketType std = new TicketType(); std.setName("Std");
		TestPendingOrders.edit(sut, userId, po -> { po.setEvent(ev); po.setTicketType(std); po.setParticipantName(null); po.setParticipantEmail(null); });
		String res = sut.provideInfo(userId, java.util.Map.of("name",""));
		assertThat(res).contains("Còn thiếu thông tin").contains("Email");
	}

	@org.junit.jupiter.api.Test
	void provideInfo_complete_confirmsAndSetsStep() {
		Event ev = new Event(); ev.setId(7L); ev.setTitle("Summit");
		TicketType std = new TicketType(); std.setName("Std"); std.setPrice(BigDecimal.valueOf(123));
		TestPendingOrders.edit(sut, userId, po -> { po.setEvent(ev); po.setTicketType(std); po.setParticipantName(null); po.setParticipantEmail(null); });
		String res = sut.provideInfo(userId, java.util.Map.of(
				"name","Alice",
				"email","a@b.com",
				"phone","0123"
		));
		assertThat(res).contains("Xác nhận thông tin đơn hàng").contains("Summit").contains("Alice");
		assertThat(sut.getPendingOrder(userId).getCurrentStep()).isEqualTo(PendingOrder.OrderStep.CONFIRM_ORDER);
	}

	// ==== cancel/get/has pending ====
	@org.junit.jupiter.api.Test
	void cancelOrder_whenPresent_thenRemoved() {
		assertThat(sut.hasPendingOrder(userId)).isTrue();
		String msg = sut.cancelOrder(userId);
		assertThat(msg).contains("Đã hủy đơn hàng");
		assertThat(sut.hasPendingOrder(userId)).isFalse();
	}

	@org.junit.jupiter.api.Test
	void cancelOrder_whenNone_thenInfo() {
		String msg = sut.cancelOrder(999L);
		assertThat(msg).contains("Không có đơn hàng nào đang chờ xử lý");
	}

	@org.junit.jupiter.api.Test
	void getAndHasPendingOrder_work() {
		assertThat(sut.hasPendingOrder(userId)).isTrue();
		assertThat(sut.getPendingOrder(userId)).isNotNull();
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
		User user = c.getUser();
		when(userRepo.findByAccount_AccountId(userId)).thenReturn(Optional.of(user));
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
		Customer c = customer();
		User user = c.getUser();
		when(userRepo.findByAccount_AccountId(userId)).thenReturn(Optional.of(user));
		when(orderService.createOrderWithTicketTypes(any(), any())).thenThrow(new RuntimeException(msg));
		Map<String, Object> res = sut.confirmOrder(userId);
		assertThat(res.get("success")).isEqualTo(false);
		assertThat(String.valueOf(res.get("message"))).contains(msg);
	}

	private Customer customer() {
		Customer c = new Customer();
		c.setCustomerId(9L);
		Account a = new Account(); 
		a.setAccountId(userId); 
		a.setEmail("cust@example.com");
		User user = new User();
		user.setAccount(a);
		user.setUserId(1L);
		c.setUser(user);
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










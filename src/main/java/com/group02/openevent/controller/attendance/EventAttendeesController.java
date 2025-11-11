package com.group02.openevent.controller.attendance;

import com.group02.openevent.model.attendance.EventAttendance;
import com.group02.openevent.service.EventAttendanceService;
import com.group02.openevent.service.EventService;
import com.group02.openevent.service.QRCodeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/event/{eventId}/attendees")
@Slf4j
public class EventAttendeesController {
    @Autowired
    private EventService eventService;

    @Autowired
    private EventAttendanceService attendanceService;

    @Autowired
    private QRCodeService qrCodeService;

    @PostMapping("/{attendeeId}/check-in")
    @ResponseBody
    public ResponseEntity<?> checkIn(
            @PathVariable Long eventId,
            @PathVariable Long attendeeId
    ) {

        try {
            EventAttendance attendance = attendanceService.listCheckIn(eventId,attendeeId);
            return ResponseEntity.ok(attendance);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
    @PostMapping("/{attendeeId}/check-out")
    @ResponseBody
    public ResponseEntity<?> checkOut(
            @PathVariable Long eventId,
            @PathVariable Long attendeeId
    ) {

        try {
            EventAttendance attendance = attendanceService.checkOut(eventId,attendeeId);
            return ResponseEntity.ok(attendance);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
    @PostMapping("/add")
    @ResponseBody
    public ResponseEntity<?> addAttendee(
            @PathVariable Long eventId,
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam Long ticketTypeId,
            @RequestParam(required = false) String organization
    ) {




        try {
            EventAttendance attendance = attendanceService.addAttendee(
                    eventId, name, email, phone, ticketTypeId, organization);
            return ResponseEntity.ok(attendance);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
    @PutMapping("/{attendeeId}/edit")
    @ResponseBody
    public ResponseEntity<?> editAttendee(
            @PathVariable Long eventId,
            @PathVariable Long attendeeId,
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam(required = false) String organization
    ) {
        try {
            EventAttendance attendance = attendanceService.updateAttendee(eventId,
                    attendeeId, name, email, phone, organization);
            return ResponseEntity.ok(attendance);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
    @DeleteMapping("/{attendeeId}")
    @ResponseBody
    public ResponseEntity<?> deleteAttendee(
            @PathVariable Long eventId,
            @PathVariable Long attendeeId
    ) {

        try {
            attendanceService.deleteAttendee(eventId,attendeeId);
            return ResponseEntity.ok("{\"message\": \"Deleted successfully\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/export/excel")
    public ResponseEntity<byte[]> exportToExcel(
            @PathVariable Long eventId,
            @RequestParam(required = false) Long ticketTypeFilter,
            @RequestParam(required = false) String paymentStatusFilter,
            @RequestParam(required = false) String checkinStatusFilter
    ) throws IOException {






        List<EventAttendance> attendees = attendanceService.filterAttendees(
                eventId, ticketTypeFilter, paymentStatusFilter, checkinStatusFilter);

        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Attendees");

        // Tạo header
        Row headerRow = sheet.createRow(0);
        String[] headers = {"STT", "Tên", "Email", "SĐT", "Loại vé", "Tổ chức", "Trạng thái thanh toán",
                "Check-in", "Check-out", "Ghi chú"};

        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            CellStyle style = workbook.createCellStyle();
            style.setFillForegroundColor(IndexedColors.GREEN.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            Font font = workbook.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            style.setFont(font);
            cell.setCellStyle(style);
        }

        // Điền dữ liệu
        int rowNum = 1;
        for (EventAttendance attendee : attendees) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(rowNum - 1);
            
            // Handle null order case (attendees added manually don't have orders)
            if (attendee.getOrder() != null) {
                // Attendee có Order (người mua vé thật)
                row.createCell(1).setCellValue(attendee.getOrder().getParticipantName() != null ? attendee.getOrder().getParticipantName() : "");
                row.createCell(2).setCellValue(attendee.getOrder().getParticipantEmail() != null ? attendee.getOrder().getParticipantEmail() : "");
                row.createCell(3).setCellValue(attendee.getOrder().getParticipantPhone() != null ? attendee.getOrder().getParticipantPhone() : "");
                row.createCell(4).setCellValue(attendee.getOrder().getTicketType() != null && attendee.getOrder().getTicketType().getName() != null ? attendee.getOrder().getTicketType().getName() : "");
                row.createCell(5).setCellValue(attendee.getOrder().getParticipantOrganization() != null ? attendee.getOrder().getParticipantOrganization() : "");
                row.createCell(6).setCellValue(attendee.getOrder().getStatus() != null ? attendee.getOrder().getStatus().toString() : "");
            } else {
                // Attendee thêm thủ công (không có Order)
                row.createCell(1).setCellValue(attendee.getFullName() != null ? attendee.getFullName() : "");
                row.createCell(2).setCellValue(attendee.getEmail() != null ? attendee.getEmail() : "");
                row.createCell(3).setCellValue(attendee.getPhone() != null ? attendee.getPhone() : "");
                row.createCell(4).setCellValue("Thêm thủ công"); // Không có ticket type
                row.createCell(5).setCellValue(attendee.getOrganization() != null ? attendee.getOrganization() : "");
                row.createCell(6).setCellValue("Không có Order"); // Không có payment status
            }
            
            row.createCell(7).setCellValue(attendee.getCheckInTime() != null ? attendee.getCheckInTime().toString() : "");
            row.createCell(8).setCellValue(attendee.getCheckOutTime() != null ? attendee.getCheckOutTime().toString() : "");
            row.createCell(9).setCellValue(attendee.getNotes() != null ? attendee.getNotes() : "");
        }

        // Adjust column width
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        workbook.write(bos);
        workbook.close();

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.add("Content-Disposition", "attachment; filename=attendees_" + eventId + ".xlsx");
        responseHeaders.setContentType(MediaType.APPLICATION_OCTET_STREAM);

        return new ResponseEntity<>(bos.toByteArray(), responseHeaders, HttpStatus.OK);
    }
}

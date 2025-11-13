package com.group02.openevent.service;

import com.group02.openevent.dto.home.TopStudentDTO;

import java.util.List;

public interface TopStudentService {
    /**
     * Get top students by counting their PAID events
     * @param limit Number of top students to return
     * @return List of TopStudentDTO, sorted by event count descending
     */
    List<TopStudentDTO> getTopStudents(int limit);
}


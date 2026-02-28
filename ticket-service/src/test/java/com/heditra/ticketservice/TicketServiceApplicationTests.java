package com.heditra.ticketservice;

import com.heditra.ticketservice.repository.TicketSearchRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class TicketServiceApplicationTests {

    @MockBean
    private TicketSearchRepository ticketSearchRepository;

    @Test
    void contextLoads() {
    }
}

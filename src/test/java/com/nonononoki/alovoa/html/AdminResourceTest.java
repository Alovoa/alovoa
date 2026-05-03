package com.nonononoki.alovoa.html;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.repo.UserRepository;
import com.nonononoki.alovoa.service.AdminService;
import com.nonononoki.alovoa.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminResourceTest {

	@Autowired
	private AdminResource adminResource;

	@Autowired
	private UserRepository userRepo;

	@MockitoBean
	private AuthService authService;

	@MockitoBean
	private AdminService adminService;

	@Test
	void test() throws Exception {
		User adminUser = userRepo.findById(1L).get();
		when(authService.getCurrentUser()).thenReturn(adminUser);
		when(authService.getCurrentUser(true)).thenReturn(adminUser);
		doNothing().when(adminService).checkRights();
		adminResource.admin();
	}
}

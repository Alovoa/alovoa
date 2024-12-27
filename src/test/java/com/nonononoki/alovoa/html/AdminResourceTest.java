package com.nonononoki.alovoa.html;

import com.nonononoki.alovoa.entity.User;
import com.nonononoki.alovoa.repo.UserRepository;
import com.nonononoki.alovoa.service.AuthService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

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

	@Test
	void test() throws Exception {
		User adminUser = userRepo.findById(1L).get();
		Mockito.when(authService.getCurrentUser()).thenReturn(adminUser);
		Mockito.when(authService.getCurrentUser(true)).thenReturn(adminUser);
		adminResource.admin();
	}
}

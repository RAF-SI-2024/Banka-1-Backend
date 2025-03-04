package com.banka1.user.service;


import com.banka1.user.DTO.request.SetPasswordRequest;
import com.banka1.user.model.SetPassword;
import com.banka1.user.repository.CustomerRepository;
import com.banka1.user.repository.SetPasswordRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;


@Slf4j
@Service
public class SetPasswordService {
	private final SetPasswordRepository setPasswordRepository;
	private final CustomerRepository customerRepository;

	public SetPasswordService(SetPasswordRepository setPasswordRepository, CustomerRepository customerRepository) {
		this.setPasswordRepository = setPasswordRepository;
		this.customerRepository = customerRepository;
	}

	public void saveSetPasswordRequest(String token, long userId) {
		var now = Instant.now();
		var setPassword = SetPassword.builder()
				.token(token)
				.userId(userId)
				.used(false)
				.createdDate(now)
				.expirationDate(Instant.now()
						.plus(Duration.ofDays(1)))
				.build();
		setPasswordRepository.save(setPassword);
	}


	public void setPassword(SetPasswordRequest dto) {
		log.debug("SetPassword called");

		var setRequest = setPasswordRepository.findByToken(dto.getToken());
		if (setRequest.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Token nije pronađen.");
		log.debug("Token valid");
		var setPassword = setRequest.get();
		if (setPassword.getExpirationDate()
				.isBefore(Instant.now())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token je istekao.");
		}
		log.debug("Token not expired");
		if (setPassword.getUsed()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Token je već korišćen.");
		}
		var customerOptional = customerRepository.findById(setPassword.getUserId());
		if (customerOptional.isEmpty())
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Korisnik nije pronađen.");
		var customer = customerOptional.get();
		var salt = generateSalt();
		customer.setPassword(BCrypt.hashpw(dto.getPassword() + salt, BCrypt.gensalt()));
		customer.setSaltPassword(salt);
		customerRepository.save(customer);
		setPassword.setUsed(true);
		setPasswordRepository.save(setPassword);
		log.debug("Password set");
	}


	private String generateSalt() {
		byte[] saltBytes = new byte[16];
		new SecureRandom().nextBytes(saltBytes);
		return Base64.getEncoder()
				.encodeToString(saltBytes);
	}
}

package com.transaction.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import com.transaction.dto.StatementDtos.StatementInitiateRequest;
import com.transaction.dto.StatementDtos.StatementInitiateResponse;
import com.transaction.dto.StatementDtos.StatementVerifyRequest;
import com.transaction.proxyService.OtpServiceClient;
import com.transaction.dto.OtpVerifyRequest;
import com.transaction.dto.OtpVerifyResponse;
import com.transaction.proxyService.UserServiceClient;
import com.transaction.proxyService.NotificationServiceClient;
import com.transaction.proxyService.AccountServiceClient;
import com.transaction.dto.UserDto;
import com.transaction.dto.AccountDto;
import com.transaction.dto.OtpGenerateRequest;
import com.transaction.dto.OtpGenerateResponse;
import com.transaction.dto.NotificationRequestDto;
import org.springframework.mail.MailException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

import com.transaction.dto.DepositRequest;
import com.transaction.dto.TransferRequest;
import com.transaction.dto.WithdrawRequest;
import com.transaction.dto.FineRequest;
import com.transaction.dto.InternalDebitRequest;
import com.transaction.dto.DebitCardWithdrawRequest;
import com.transaction.exceptions.AccountNotFoundException;
import com.transaction.exceptions.InsufficientFundsException;
import com.transaction.exceptions.InvalidTransactionException;
import com.transaction.exceptions.TransactionProcessingException;
import com.transaction.model.Transaction;
import com.transaction.service.TransactionService;

import jakarta.validation.Valid; // For input validation

@RestController // Marks this class as a REST controller, handling incoming HTTP requests
@RequestMapping("/transactions") // Base path for all endpoints in this controller
public class TransactionController {
	
	@Autowired
    private final TransactionService transactionService;

    // Added for statements OTP + email
    @Autowired
    private OtpServiceClient otpServiceClient;

    @Autowired
    private UserServiceClient userServiceClient;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private NotificationServiceClient notificationServiceClient;

    @Autowired
    private AccountServiceClient accountServiceClient;

    @Autowired // Injects the TransactionService implementation
    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    /**
     * Handles POST /transactions/deposit requests.
     * Facilitates depositing funds into an account.
     * @param request The DepositRequest DTO containing account ID and amount.
     * @return ResponseEntity with the created Transaction and HTTP status 201 (Created).
     * @throws AccountNotFoundException if the target account does not exist.
     * @throws TransactionProcessingException if the deposit fails.
     * (Other exceptions are handled by GlobalExceptionHandler)
     */
    @PostMapping("/deposit")
    public ResponseEntity<Transaction> deposit(@Valid @RequestBody DepositRequest request) {
        // @Valid triggers validation defined in DepositRequest DTO
        // Exceptions are thrown by the service layer and caught by GlobalExceptionHandler
        Transaction transaction = transactionService.deposit(request);
        return new ResponseEntity<>(transaction, HttpStatus.CREATED);
    }

    /**
     * Handles POST /transactions/withdraw requests.
     * Facilitates withdrawing funds from an account.
     * @param request The WithdrawRequest DTO containing account ID and amount.
     * @return ResponseEntity with the created Transaction and HTTP status 201 (Created).
     * @throws AccountNotFoundException if the source account does not exist.
     * @throws InsufficientFundsException if the account has insufficient funds.
     * @throws TransactionProcessingException if the withdrawal fails.
     */
    @PostMapping("/withdraw")
    public ResponseEntity<Transaction> withdraw(@Valid @RequestBody WithdrawRequest request) {
        Transaction transaction = transactionService.withdraw(request);
        return new ResponseEntity<>(transaction, HttpStatus.CREATED);
    }

    /**
     * Handles POST /transactions/transfer requests.
     * Facilitates fund transfer between accounts.
     * @param request The TransferRequest DTO containing fromAccountId, toAccountId, and amount.
     * @return ResponseEntity with the created Transaction and HTTP status 201 (Created).
     * @throws AccountNotFoundException if source or target account does not exist.
     * @throws InsufficientFundsException if the source account has insufficient funds.
     * @throws InvalidTransactionException if attempting to transfer to the same account.
     * @throws TransactionProcessingException if the transfer fails.
     */
    @PostMapping("/transfer")
    public ResponseEntity<Transaction> transfer(@Valid @RequestBody TransferRequest request) {
        Transaction transaction = transactionService.transfer(request);
        return new ResponseEntity<>(transaction, HttpStatus.CREATED);
    }

    /**
     * Handles POST /transactions/fine requests.
     * Internal endpoint to record a fine transaction (no OTP/KYC).
     * @param request The FineRequest containing account ID, amount, and optional message.
     * @return ResponseEntity with the created Transaction and HTTP status 201 (Created).
     */
    @PostMapping("/fine")
    public ResponseEntity<Transaction> recordFine(@Valid @RequestBody FineRequest request) {
        Transaction transaction = transactionService.recordFine(request);
        return new ResponseEntity<>(transaction, HttpStatus.CREATED);
    }

    /**
     * Handles POST /transactions/internal/debit requests.
     * Internal endpoint used by other services (e.g., CreditCardService) to debit fees such as CARD_ISSUANCE_FEE.
     * Requires a valid JWT (enforced by SecurityConfig). No OTP required.
     */
    @PostMapping("/internal/debit")
    public ResponseEntity<Transaction> internalDebit(@Valid @RequestBody InternalDebitRequest request) {
        Transaction transaction = transactionService.internalDebit(request);
        return new ResponseEntity<>(transaction, HttpStatus.CREATED);
    }

    /**
     * Handles POST /transactions/debit-card/withdraw requests.
     * Performs withdrawal using a debit card. Validates card with CreditCardService and requires OTP.
     */
    @PostMapping("/debit-card/withdraw")
    public ResponseEntity<Transaction> debitCardWithdraw(@Valid @RequestBody DebitCardWithdrawRequest request) {
        Transaction transaction = transactionService.debitCardWithdraw(request);
        return new ResponseEntity<>(transaction, HttpStatus.CREATED);
    }

    /**
     * Handles GET /transactions/account/{id} requests.
     * Retrieves all transactions for a specific account.
     * @param accountId The ID of the account.
     * @return ResponseEntity with a list of Transaction entities and HTTP status 200 (OK).
     */
    @GetMapping("/account/{accountId}")
    public ResponseEntity<List<Transaction>> getTransactionsByAccountId(@PathVariable String accountId) {
        List<Transaction> transactions = transactionService.getTransactionsByAccountId(accountId);
        if (transactions.isEmpty()) {
            // Optionally return 404 if no transactions are found for the account,
            // or 200 with an empty list depending on API design preference.
            // For now, returning 200 OK with an empty list is common.
            return new ResponseEntity<>(HttpStatus.NO_CONTENT); // Or HttpStatus.OK with empty list
        }
        return new ResponseEntity<>(transactions, HttpStatus.OK);
    }

    /**
     * Handles GET /transactions/{transactionId} requests.
     * Retrieves a single transaction by its ID.
     * @param transactionId The ID of the transaction.
     * @return ResponseEntity with the Transaction entity and HTTP status 200 (OK),
     * or 404 (Not Found) if the transaction does not exist.
     */
    @GetMapping("/{transactionId}")
    public ResponseEntity<Transaction> getTransactionById(@PathVariable String transactionId) {
        Optional<Transaction> transaction = transactionService.getTransactionById(transactionId);
        return transaction.map(value -> new ResponseEntity<>(value, HttpStatus.OK))
                          .orElseGet(() -> new ResponseEntity<>(HttpStatus.NOT_FOUND));
    }

    // ===== Account Statement with OTP + Password-protected PDF (emailed) =====

    // Step 1: Initiate OTP to provided email (or fallback to user's registered email)
    @PostMapping("/statements/initiate")
    public ResponseEntity<StatementInitiateResponse> initiateStatement(@Valid @RequestBody StatementInitiateRequest request) {
        // Resolve recipient email
        String recipient = request.getToEmail();
        if (recipient == null || recipient.isBlank()) {
            UserDto profile = userServiceClient.getUserProfileById(request.getUserId());
            if (profile == null || profile.getEmail() == null || profile.getEmail().isBlank()) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
            recipient = profile.getEmail();
        }

        OtpGenerateRequest gen = new OtpGenerateRequest();
        gen.setUserId(recipient); // public flow addressed to email
        gen.setPurpose("CONTACT_VERIFICATION");
        gen.setChannels(java.util.List.of("EMAIL"));
        gen.setContextId(statementContextId(request.getAccountId(), request.getFromDate(), request.getToDate()));

        OtpGenerateResponse resp = otpServiceClient.generatePublic(gen);
        StatementInitiateResponse out = new StatementInitiateResponse(
                resp != null ? resp.getRequestId() : null,
                resp != null ? resp.getExpiresAt() : null
        );
        return new ResponseEntity<>(out, HttpStatus.CREATED);
    }

    // Step 2: Verify OTP, generate password-protected PDF, email to recipient
    @PostMapping("/statements/verify")
    public ResponseEntity<?> verifyAndSendStatement(@Valid @RequestBody StatementVerifyRequest request) {
        // Resolve recipient email
        String recipient = request.getToEmail();
        if (recipient == null || recipient.isBlank()) {
            UserDto profile = userServiceClient.getUserProfileById(request.getUserId());
            if (profile == null || profile.getEmail() == null || profile.getEmail().isBlank()) {
                return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            }
            recipient = profile.getEmail();
        }

        OtpVerifyRequest v = new OtpVerifyRequest(
            recipient,
            "CONTACT_VERIFICATION",
            statementContextId(request.getAccountId(), request.getFromDate(), request.getToDate()),
            request.getCode()
        );
        OtpVerifyResponse vResp = otpServiceClient.verifyPublic(v);
        if (vResp == null || !vResp.isVerified()) {
            return new ResponseEntity<>(vResp, HttpStatus.BAD_REQUEST);
        }

        // Collect transactions within date range
        List<Transaction> all = transactionService.getTransactionsByAccountId(request.getAccountId());
        LocalDateTime start = request.getFromDate().atStartOfDay();
        LocalDateTime end = request.getToDate().atTime(23, 59, 59);
        List<Transaction> filtered = all.stream()
                .filter(t -> t.getTransactionDate() != null
                        && !t.getTransactionDate().isBefore(start)
                        && !t.getTransactionDate().isAfter(end))
                .collect(Collectors.toList());

        // Determine password FIRST4NAME + YYYY
        UserDto owner = userServiceClient.getUserProfileById(request.getUserId());
        String pass = buildPassword(owner);

        try {
            AccountDto account = accountServiceClient.getAccountById(request.getAccountId());
            byte[] pdfBytes = generateStatementPdf(filtered, request.getAccountId(), request.getFromDate(), request.getToDate(), owner, account);
            byte[] protectedPdf = protectPdf(pdfBytes, pass);

            // Email the PDF to recipient
            sendStatementEmail(recipient, protectedPdf, request.getAccountId(), request.getFromDate(), request.getToDate());

            return ResponseEntity.ok(vResp);
        } catch (MailException mailEx) {
            // Fallback: notify user via NotificationService and avoid 500
            try {
                NotificationRequestDto notificationRequest = new NotificationRequestDto(
                        request.getUserId(),
                        NotificationRequestDto.NotificationType.EMAIL,
                        "Your account statement was generated but email delivery failed due to mail server configuration. " +
                                "An administrator will resend it shortly."
                );
                notificationServiceClient.sendEmailNotification(notificationRequest);
            } catch (Exception ignore) { }
            return new ResponseEntity<>("Statement generated but email delivery failed; admin will resend.", HttpStatus.ACCEPTED);
        } catch (Exception e) {
            return new ResponseEntity<>("Failed to generate or send statement: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String statementContextId(String accountId, LocalDate from, LocalDate to) {
        return "STATEMENT:" + accountId + ":" + from + "_" + to;
    }

    private String buildPassword(UserDto user) {
        String first = (user != null && user.getFirstName() != null) ? user.getFirstName().toUpperCase() : "USER";
        String four = first.length() >= 4 ? first.substring(0, 4) : String.format("%-4s", first).replace(' ', 'X');
        String yyyy = "2000";
        if (user != null && user.getDateOfBirth() != null) {
            try {
                yyyy = String.valueOf(user.getDateOfBirth().getYear());
            } catch (Exception ignore) {}
        }
        return four + yyyy;
    }

    private byte[] generateStatementPdf(List<Transaction> txs, String accountId, LocalDate from, LocalDate to, UserDto owner, AccountDto account) throws Exception {
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(doc, page);
            try {
                float margin = 50;
                float y = page.getMediaBox().getHeight() - margin;

                // Title
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 16);
                cs.newLineAtOffset(margin, y);
                cs.showText("Account Statement");
                cs.endText();
                y -= 24;

                // Period
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA, 11);
                cs.newLineAtOffset(margin, y);
                cs.showText("Period: " + from + " to " + to);
                cs.endText();
                y -= 16;

                // User details
                if (owner != null) {
                    String fullName = (owner.getFirstName() != null ? owner.getFirstName() : "")
                            + " " + (owner.getLastName() != null ? owner.getLastName() : "");
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 10);
                    cs.newLineAtOffset(margin, y);
                    cs.showText("User: " + fullName.trim());
                    cs.endText();
                    y -= 12;

                    if (owner.getEmail() != null) {
                        cs.beginText();
                        cs.setFont(PDType1Font.HELVETICA, 10);
                        cs.newLineAtOffset(margin, y);
                        cs.showText("Email: " + owner.getEmail());
                        cs.endText();
                        y -= 12;
                    }
                    try {
                        if (owner.getDateOfBirth() != null) {
                            cs.beginText();
                            cs.setFont(PDType1Font.HELVETICA, 10);
                            cs.newLineAtOffset(margin, y);
                            cs.showText("Date of Birth: " + owner.getDateOfBirth());
                            cs.endText();
                            y -= 12;
                        }
                    } catch (Exception ignore) {}
                }

                // Account details
                if (account != null) {
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 10);
                    cs.newLineAtOffset(margin, y);
                    cs.showText("Account Number: " + (account.getAccountNumber() != null ? account.getAccountNumber() : accountId));
                    cs.endText();
                    y -= 12;

                    if (account.getAccountType() != null) {
                        cs.beginText();
                        cs.setFont(PDType1Font.HELVETICA, 10);
                        cs.newLineAtOffset(margin, y);
                        cs.showText("Account Type: " + account.getAccountType());
                        cs.endText();
                        y -= 12;
                    }

                    try {
                        if (account.getBalance() != null) {
                            cs.beginText();
                            cs.setFont(PDType1Font.HELVETICA, 10);
                            cs.newLineAtOffset(margin, y);
                            cs.showText(String.format("Current Balance: %.2f", account.getBalance()));
                            cs.endText();
                            y -= 12;
                        }
                    } catch (Exception ignore) {}
                }

                y -= 4;
                // Header for transaction table
                cs.beginText();
                cs.setFont(PDType1Font.HELVETICA_BOLD, 10);
                cs.newLineAtOffset(margin, y);
                cs.showText("Date/Time          Type       Amount       Status      From            To              TxnId");
                cs.endText();
                y -= 14;

                // Rows (truncate to page height)
                for (Transaction t : txs) {
                    if (y < 60) break; // avoid overflow
                    String fromId = t.getFromAccountId() != null ? t.getFromAccountId() : "-";
                    String toId = t.getToAccountId() != null ? t.getToAccountId() : "-";
                    String line = String.format("%-19s %-10s %-12.2f %-11s %-14s %-14s %s",
                            t.getTransactionDate(),
                            t.getType(),
                            t.getAmount(),
                            t.getStatus(),
                            fromId.length() > 12 ? fromId.substring(0, 12) + "…" : fromId,
                            toId.length() > 12 ? toId.substring(0, 12) + "…" : toId,
                            t.getTransactionId());
                    cs.beginText();
                    cs.setFont(PDType1Font.HELVETICA, 9);
                    cs.newLineAtOffset(margin, y);
                    cs.showText(line);
                    cs.endText();
                    y -= 12;
                }
            } finally {
                cs.close();
            }
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    private byte[] protectPdf(byte[] rawPdf, String userPassword) throws Exception {
        try (PDDocument doc = PDDocument.load(rawPdf); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            AccessPermission ap = new AccessPermission();
            String ownerPassword = java.util.UUID.randomUUID().toString();
            StandardProtectionPolicy spp = new StandardProtectionPolicy(ownerPassword, userPassword, ap);
            spp.setEncryptionKeyLength(128);
            spp.setPermissions(ap);
            doc.protect(spp);
            doc.save(out);
            return out.toByteArray();
        }
    }

    private void sendStatementEmail(String toEmail, byte[] pdfBytes, String accountId, LocalDate from, LocalDate to) throws Exception {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        // Ensure 'From' is set to the configured SMTP username (required by many providers incl. Gmail)
        try {
            if (mailSender instanceof org.springframework.mail.javamail.JavaMailSenderImpl senderImpl) {
                String fromAddr = senderImpl.getUsername();
                if (fromAddr != null && !fromAddr.isBlank()) {
                    helper.setFrom(fromAddr);
                }
            }
        } catch (Exception ignore) {}

        helper.setTo(toEmail);
        helper.setSubject("Your Account Statement (" + accountId + ")");
        String body = "Please find your password-protected statement attached.\n"
                + "Period: " + from + " to " + to + ".\n\n"
                + "Password to open the PDF: FIRST 4 letters of your first name in UPPERCASE + YEAR of birth (e.g., ABCD2003).\n"
                + "If your first name has fewer than 4 letters, remaining letters are replaced with 'X' (e.g., JO -> JOXX).";
        helper.setText(body, false);
        String filename = "statement_" + accountId + "_" + from + "_" + to + ".pdf";
        helper.addAttachment(filename, new org.springframework.core.io.ByteArrayResource(pdfBytes));
        mailSender.send(message);
    }
}

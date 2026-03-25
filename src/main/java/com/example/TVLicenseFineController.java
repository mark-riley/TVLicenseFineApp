package com.example;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/")
public class TVLicenseFineController {

    private final TVLicenseFineRepository repo;
    private final TVLicenseTransactionRepository transactionRepo;
    private final TVLicenseStatusRepository statusRepo;

    public TVLicenseFineController(TVLicenseFineRepository repo,
                                   TVLicenseTransactionRepository transactionRepo,
                                   TVLicenseStatusRepository statusRepo) {
        this.repo = repo;
        this.transactionRepo = transactionRepo;
        this.statusRepo = statusRepo;
        // Initialize Stripe with your Secret Key
        Stripe.apiKey = "sk_test_51TEVGrCt5TY5B9EcCm2HAPmwsQGYOGmlhtARd1lWEUMQr1fbYhVuGQAcfnkb5bJVzZryP6SWAPiplwh0egdncnj400TKnGwUe6";
    }

    @GetMapping
    public String home() {
        return "fines/find";
    }

    @PostMapping("/find-fine")
    public String processSearch(@RequestParam String reference, @RequestParam String postcode, Model model) {
        List<TVLicenseFine> fines = repo.findByReferenceAndPostcode(reference, postcode);
        if (!fines.isEmpty()) {
            model.addAttribute("fine", fines.get(0));
            return "fines/make";
        }
        return "fines/find";
    }

    @PostMapping("/submit-payment")
    public String submitPayment(@RequestParam Long fineId,
                                @RequestParam BigDecimal amountPaid,
                                HttpServletRequest request) throws Exception {

        long amountInPence = amountPaid.multiply(new BigDecimal("100")).longValue();

        // Dynamically get the base URL (e.g., http://localhost:8080)
        String baseUrl = String.format("%s://%s:%d", request.getScheme(), request.getServerName(), request.getServerPort());

        SessionCreateParams params = SessionCreateParams.builder()
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                // Redirect back to our confirmation page with the session ID
                .setSuccessUrl(baseUrl + "/confirmation?session_id={CHECKOUT_SESSION_ID}&fineId=" + fineId)
                .setCancelUrl(baseUrl + "/make")
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("gbp")
                                .setUnitAmount(amountInPence)
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName("TV License Fine: " + fineId)
                                        .build())
                                .build())
                        .build())
                .build();

        Session session = Session.create(params);
        return "redirect:" + session.getUrl();
    }

    @GetMapping("/confirmation")
    public String confirmation(@RequestParam("session_id") String sessionId,
                               @RequestParam("fineId") Long fineId,
                               Model model) throws Exception {

        // 1. Retrieve the session from Stripe to verify payment status
        Session session = Session.retrieve(sessionId);

        if ("paid".equals(session.getPaymentStatus())) {
            Optional<TVLicenseFine> fineOpt = repo.findById(fineId);
            BigDecimal amountPaid = new BigDecimal(session.getAmountTotal()).divide(new BigDecimal(100));

            if (fineOpt.isPresent()) {
                TVLicenseFine fine = fineOpt.get();

                // 2. Update the fine status to "Paid" (Assumes ID 3 is Paid)
                TVLicenseStatus paidStatus = statusRepo.findById(3L).orElse(null);
                if (paidStatus != null) {
                    fine.setStatus(paidStatus);
                    repo.save(fine);
                }

                // 3. Save the Transaction Record
                TVLicenseTransaction transaction = new TVLicenseTransaction(
                        fine,
                        amountPaid,
                        "Stripe",
                        sessionId, // Using Stripe session ID as the processor token
                        LocalDateTime.now()
                );
                transactionRepo.save(transaction);

                model.addAttribute("fine", fine);
            }

            model.addAttribute("amountPaid", amountPaid);
            model.addAttribute("transactionId", sessionId);
            return "fines/confirmation";
        }

        return "redirect:/find-fine?error=payment_failed";
    }
}
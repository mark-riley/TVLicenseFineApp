package com.example;

import com.stripe.Stripe;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import com.stripe.param.checkout.SessionRetrieveParams;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/")
public class TVLicenseFineController {

    private final TVLicenseFineRepository repo;

    public TVLicenseFineController(TVLicenseFineRepository repo) {
        this.repo = repo;
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
                                @RequestParam String email) throws Exception {

        // Stripe expects amounts in cents (long)
        long amountInCents = amountPaid.multiply(new BigDecimal("100")).longValue();

        SessionCreateParams params = SessionCreateParams.builder()
                .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
                .setMode(SessionCreateParams.Mode.PAYMENT)
                // Redirect back to our confirmation page with the session ID
                .setSuccessUrl("http://localhost:8080/confirmation?session_id={CHECKOUT_SESSION_ID}&fineId=" + fineId)
                .setCancelUrl("http://localhost:8080/make")
                .setCustomerEmail(email)
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("gbp")
                                .setUnitAmount(amountInCents)
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
            // 2. Update the fine status in your H2 Database
            Optional<TVLicenseFine> fineOpt = repo.findById(fineId);
            if (fineOpt.isPresent()) {
                TVLicenseFine fine = fineOpt.get();
                // Assumes your TVLicenseStatus has a "Paid" state (ID 2 or similar)
                // fine.setStatus(new TVLicenseStatus(2, "Paid"));
                // repo.save(fine);
                model.addAttribute("fine", fine);
            }
            model.addAttribute("amountPaid", new BigDecimal(session.getAmountTotal()).divide(new BigDecimal(100)));
            return "fines/confirmation";
        }

        return "redirect:/fines/find?error=payment_failed";
    }
}
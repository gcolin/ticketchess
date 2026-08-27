package com.github.gcolin.payment;

import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.StripeCollection;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionListParams;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Exports Stripe Checkout sessions to CSV. Usage: {@code StripeSession <apiKey> [out.csv]}
 */
public class StripeSession {

    public static void main(String[] args) throws StripeException, IOException {
        if (args.length < 1 || args[0].isBlank()) {
            System.err.println("Usage: StripeSession <stripe-secret-api-key> [out.csv]");
            System.exit(1);
        }

        String apiKey = args[0];
        String output = args.length > 1 ? args[1] : "out.csv";
        StripeClient client = new StripeClient(apiKey);

        int limit = 100;
        String last = null;
        StringBuilder str = new StringBuilder();
        str.append("id,email,amount,intent\n");
        while (true) {
            SessionListParams.Builder params = SessionListParams.builder().setLimit((long) limit);
            if (last != null) {
                params.setStartingAfter(last);
            }
            StripeCollection<Session> list = client.v1().checkout().sessions().list(params.build());
            for (Session session : list.getData()) {
                str.append(session.getId()).append(",");
                str.append(session.getMetadata().toString()).append(",");
                str.append(session.getAmountTotal()).append(",");
                str.append(session.getPaymentIntent()).append("\n");
            }
            if (list.getData().size() == limit) {
                last = list.getData().get(limit - 1).getId();
            } else {
                break;
            }
        }

        Files.write(Paths.get(output), str.toString().getBytes(StandardCharsets.UTF_8));
    }
}

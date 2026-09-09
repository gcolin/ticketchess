package com.github.gcolin.payment;

import com.github.gcolin.membership.Membership;
import com.github.gcolin.membership.MembershipOptionSubscription;
import com.github.gcolin.platform.Config;
import com.github.gcolin.platform.LogoService;
import com.github.gcolin.platform.ServiceUtils;
import com.github.gcolin.platform.SignatureService;
import com.github.gcolin.player.Find;
import com.github.gcolin.player.IPlayer;
import com.github.gcolin.registration.PlayerSubscription;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;
import org.openpdf.text.Element;
import org.openpdf.text.Font;
import org.openpdf.text.Image;
import org.openpdf.text.PageSize;
import org.openpdf.text.Paragraph;
import org.openpdf.text.Phrase;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;
import org.openpdf.text.pdf.PdfWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PaymentReceiptPdfService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentReceiptPdfService.class);
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String[] UNITS = {
        "zéro",
        "un",
        "deux",
        "trois",
        "quatre",
        "cinq",
        "six",
        "sept",
        "huit",
        "neuf",
        "dix",
        "onze",
        "douze",
        "treize",
        "quatorze",
        "quinze",
        "seize",
        "dix-sept",
        "dix-huit",
        "dix-neuf"
    };
    private static final String[] TENS = {
        "", "", "vingt", "trente", "quarante", "cinquante", "soixante", "soixante", "quatre-vingt", "quatre-vingt"
    };

    private Properties properties;
    private LogoService logoService;
    private SignatureService signatureService;

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    public void setLogoService(LogoService logoService) {
        this.logoService = logoService;
    }

    public void setSignatureService(SignatureService signatureService) {
        this.signatureService = signatureService;
    }

    public byte[] generatePaymentReceipt(
            Payment payment,
            List<PlayerSubscription> subs,
            List<Membership> memberships,
            Map<Integer, List<MembershipOptionSubscription>> optionSubscriptions,
            Find find,
            String fallbackClientName)
            throws Exception {
        Document document = new Document(PageSize.A4, 50, 50, 45, 45);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);
        document.open();

        Font clubFont = new Font(Font.HELVETICA, 14, Font.BOLD);
        Font titleFont = new Font(Font.HELVETICA, 15, Font.BOLD);
        Font emphasisFont = new Font(Font.HELVETICA, 12, Font.BOLD);
        Font amountFont = new Font(Font.HELVETICA, 16, Font.BOLD);
        Font normalFont = new Font(Font.HELVETICA, 10);
        Font smallFont = new Font(Font.HELVETICA, 9);
        Font italicSmall = new Font(Font.HELVETICA, 8, Font.ITALIC);

        SellerInfo seller = resolveSeller();
        String invoicePrefix = Config.configured(properties, "invoice.number.prefix", null);
        if (invoicePrefix.isBlank()) {
            invoicePrefix = "FAC-";
        }
        String invoiceNumber = invoicePrefix + payment.getId();
        String paymentDate = resolvePaymentDate(payment);
        String generatedDate = LocalDateTime.now().format(DATE_FORMAT);
        String client = resolveClientName(payment, subs, memberships, find, fallbackClientName);
        String totalStr = formatAmount(payment);

        addHeader(document, seller, clubFont, smallFont);
        addCenteredLine(document, "Reçu de paiement", titleFont, 12f, 2f);
        addCenteredLine(document, "Référence n°" + invoiceNumber + " — " + paymentDate, normalFont, 0f, 10f);

        String association = isBlank(seller.name()) ? "L'association" : "L'association « " + seller.name() + " »";
        addLine(document, association + " atteste avoir reçu de la part de :", normalFont, 0f, 4f);
        addCenteredLine(document, client, emphasisFont, 0f, 6f);
        addLine(document, "un paiement d'un montant de :", normalFont, 0f, 3f);
        addCenteredLine(document, totalStr, amountFont, 0f, 6f);
        addLine(document, "pour le motif suivant :", normalFont, 0f, 4f);

        for (String motifLine : buildInvoiceMotifLines(subs, memberships, optionSubscriptions, find)) {
            addLine(document, motifLine, normalFont, 0f, 2f);
        }
        addLine(document, "Mode de paiement : " + translatePaymentType(payment.getType()), smallFont, 4f, 6f);
        addCenteredLine(document, "(Reçu généré le " + generatedDate + ")", italicSmall, 0f, 4f);

        addLine(document, seller.vatNotice(), smallFont, 0f, 2f);
        if (!isBlank(seller.footer()) && !seller.footer().equals(seller.name())) {
            addLine(document, seller.footer(), smallFont, 0f, 2f);
        }

        addSignatureBlock(document, seller.name(), null, null, smallFont);

        document.close();
        return baos.toByteArray();
    }

    public byte[] generateDonationAttestation(Payment payment) throws Exception {
        Document document = new Document(PageSize.A4, 50, 50, 45, 45);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, baos);
        document.open();

        Font clubFont = new Font(Font.HELVETICA, 14, Font.BOLD);
        Font titleFont = new Font(Font.HELVETICA, 14, Font.BOLD);
        Font emphasisFont = new Font(Font.HELVETICA, 11, Font.BOLD);
        Font amountFont = new Font(Font.HELVETICA, 15, Font.BOLD);
        Font normalFont = new Font(Font.HELVETICA, 10);
        Font smallFont = new Font(Font.HELVETICA, 9);
        Font italicSmall = new Font(Font.HELVETICA, 8, Font.ITALIC);

        SellerInfo seller = resolveSeller();
        String donationObject = Config.configured(properties, "invoice.donation.object", null);
        String signatory = Config.configured(properties, "invoice.donation.signatory", null);
        String signatoryTitle = Config.configured(properties, "invoice.donation.signatoryTitle", null);
        String donationPrefix = Config.configured(properties, "invoice.donation.prefix", null);
        if (donationPrefix.isBlank()) {
            donationPrefix = "DON-";
        }
        String attestationNumber = donationPrefix + payment.getId();
        String paymentDate = resolvePaymentDate(payment);
        String generatedDate = LocalDateTime.now().format(DATE_FORMAT);

        double amountEuros = payment.getAmountCents() != null
                ? ServiceUtils.toEuros(payment.getAmountCents())
                : (payment.getAmount() != null ? payment.getAmount() : 0d);
        String totalStr = formatEuros(amountEuros);
        String amountInWords = amountToFrenchWords(amountEuros);

        String donorName = isBlank(payment.getPayerName()) ? payment.getUserEmail() : payment.getPayerName();
        String donorAddress = payment.getPayerAddress();

        addHeader(document, seller, clubFont, smallFont);
        addCenteredLine(document, "Attestation de don aux œuvres", titleFont, 12f, 1f);
        addCenteredLine(document, "ou organismes d'intérêt général", titleFont, 0f, 2f);
        addCenteredLine(document, "Référence n°" + attestationNumber + " — " + paymentDate, normalFont, 0f, 1f);
        addCenteredLine(document, "(Articles 200 et 238 bis du Code général des impôts)", italicSmall, 0f, 10f);

        addLine(document, "Bénéficiaire du don :", emphasisFont, 0f, 2f);
        addLine(document, seller.name(), normalFont, 0f, 1f);
        addLine(document, seller.addressLine(), smallFont, 0f, 1f);
        if (!isBlank(seller.siret()) || !isBlank(seller.rna())) {
            addLine(
                    document,
                    joinNotBlank(
                            "  ",
                            isBlank(seller.siret()) ? "" : "SIRET : " + seller.siret(),
                            isBlank(seller.rna()) ? "" : "RNA : " + seller.rna()),
                    smallFont,
                    0f,
                    1f);
        }
        if (!isBlank(donationObject)) {
            addLine(document, "Objet : " + donationObject, smallFont, 0f, 1f);
        }

        addLine(document, "Donateur :", emphasisFont, 8f, 2f);
        addCenteredLine(document, donorName, emphasisFont, 0f, 1f);
        if (!isBlank(donorAddress)) {
            for (String line : donorAddress.replace("\r\n", "\n").split("\n")) {
                addCenteredLine(document, line.trim(), normalFont, 0f, 1f);
            }
        }

        addLine(document, "Le bénéficiaire reconnaît avoir reçu à titre de don manuel :", normalFont, 8f, 3f);
        addCenteredLine(document, totalStr, amountFont, 0f, 1f);
        addCenteredLine(document, "(" + amountInWords + ")", normalFont, 0f, 5f);

        addLine(document, "Date du don : " + paymentDate, normalFont, 0f, 1f);
        addLine(document, "Mode de paiement : " + translatePaymentType(payment.getType()), normalFont, 0f, 1f);
        addLine(document, "Nature du don : Don manuel", normalFont, 0f, 6f);

        addLine(
                document,
                "Le bénéficiaire certifie sur l'honneur que les dons et versements qu'il reçoit ouvrent droit "
                        + "à la réduction d'impôt prévue aux articles 200 et 238 bis du Code général des impôts.",
                smallFont,
                0f,
                3f);
        addLine(
                document,
                "Réduction d'impôt : 66 % du montant pour les particuliers (art. 200 CGI), "
                        + "60 % pour les entreprises (art. 238 bis CGI), dans la limite des plafonds légaux.",
                italicSmall,
                0f,
                5f);
        addCenteredLine(document, "(Attestation générée le " + generatedDate + ")", italicSmall, 0f, 4f);

        addSignatureBlock(document, seller.name(), signatory, signatoryTitle, smallFont);

        document.close();
        return baos.toByteArray();
    }

    private void addHeader(Document document, SellerInfo seller, Font clubFont, Font smallFont)
            throws DocumentException {
        Image logo = loadImage(resolveLogoFile(), 70f, 55f);
        Paragraph clubInfo = new Paragraph();
        clubInfo.setLeading(smallFont.getSize() + 3f);
        if (!isBlank(seller.name())) {
            clubInfo.add(new Phrase(seller.name() + "\n", clubFont));
        }
        if (!isBlank(seller.addressLine())) {
            clubInfo.add(new Phrase(seller.addressLine() + "\n", smallFont));
        }
        if (!isBlank(seller.contactLine())) {
            clubInfo.add(new Phrase(seller.contactLine() + "\n", smallFont));
        }
        if (!isBlank(seller.legalLine())) {
            clubInfo.add(new Phrase(seller.legalLine() + "\n", smallFont));
        }
        if (!isBlank(seller.prefecture())) {
            clubInfo.add(new Phrase(seller.prefecture(), smallFont));
        }

        if (logo == null) {
            clubInfo.setAlignment(Element.ALIGN_CENTER);
            clubInfo.setSpacingAfter(6f);
            document.add(clubInfo);
            return;
        }

        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100f);
        header.setWidths(new float[] {22f, 78f});
        header.setSpacingAfter(8f);

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(PdfPCell.NO_BORDER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        logoCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        logoCell.setPaddingRight(8f);
        logoCell.addElement(logo);

        PdfPCell infoCell = new PdfPCell();
        infoCell.setBorder(PdfPCell.NO_BORDER);
        infoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        infoCell.addElement(clubInfo);

        header.addCell(logoCell);
        header.addCell(infoCell);
        document.add(header);
    }

    private void addSignatureBlock(
            Document document, String sellerName, String signatory, String signatoryTitle, Font smallFont)
            throws DocumentException {
        addSignatureImage(document);
        String signedByText;
        if (!isBlank(signatory) || !isBlank(signatoryTitle)) {
            signedByText = joinNotBlank(", ", signatory, signatoryTitle);
        } else if (!isBlank(sellerName) && signatureService != null && signatureService.exists()) {
            signedByText = sellerName;
        } else {
            return;
        }
        Paragraph signedBy = new Paragraph(signedByText, smallFont);
        signedBy.setAlignment(Element.ALIGN_RIGHT);
        signedBy.setSpacingBefore(0f);
        signedBy.setSpacingAfter(0f);
        document.add(signedBy);
    }

    public static String translatePaymentType(PaymentType type) {
        if (type == null) {
            return "-";
        }
        return switch (type) {
            case CARD -> "Carte bancaire";
            case BANK_TRANSFER -> "Virement bancaire";
            case FREE -> "Gratuit";
            case CASH -> "Espèces";
            case CHEQUE -> "Chèque";
        };
    }

    static String amountToFrenchWords(double amountEuros) {
        long centsTotal = Math.round(Math.abs(amountEuros) * 100);
        long euros = centsTotal / 100;
        int cents = (int) (centsTotal % 100);
        String euroPart = numberToFrench(euros) + (euros <= 1 ? " euro" : " euros");
        if (cents == 0) {
            return euroPart;
        }
        String centPart = numberToFrench(cents) + (cents <= 1 ? " centime" : " centimes");
        return euroPart + " et " + centPart;
    }

    private static String numberToFrench(long number) {
        if (number == 0) {
            return UNITS[0];
        }
        if (number < 0 || number > 999_999) {
            return String.valueOf(number);
        }
        StringBuilder sb = new StringBuilder();
        long thousands = number / 1000;
        long remainder = number % 1000;
        if (thousands > 0) {
            if (thousands == 1) {
                sb.append("mille");
            } else {
                sb.append(belowThousand((int) thousands)).append(" mille");
            }
            if (remainder > 0) {
                sb.append(' ');
            }
        }
        if (remainder > 0) {
            sb.append(belowThousand((int) remainder));
        }
        return sb.toString();
    }

    private static String belowThousand(int number) {
        if (number < 20) {
            return UNITS[number];
        }
        int hundreds = number / 100;
        int remainder = number % 100;
        StringBuilder sb = new StringBuilder();
        if (hundreds > 0) {
            if (hundreds == 1) {
                sb.append("cent");
            } else {
                sb.append(UNITS[hundreds]).append(" cent");
            }
            if (remainder == 0 && hundreds > 1) {
                sb.append('s');
            }
            if (remainder > 0) {
                sb.append(' ');
            }
        }
        if (remainder > 0) {
            sb.append(belowHundred(remainder));
        }
        return sb.toString();
    }

    private static String belowHundred(int number) {
        if (number < 20) {
            return UNITS[number];
        }
        int tens = number / 10;
        int units = number % 10;
        if (tens == 7 || tens == 9) {
            int base = tens == 7 ? 60 : 80;
            int rest = number - base;
            String prefix = tens == 7 ? "soixante" : "quatre-vingt";
            if (rest == 0) {
                return tens == 9 ? "quatre-vingts" : "soixante-dix";
            }
            if (tens == 7 && rest == 11) {
                return "soixante et onze";
            }
            if (tens == 7 && rest == 1) {
                return "soixante et un";
            }
            return prefix + "-" + (rest < 20 ? UNITS[rest] : belowHundred(rest));
        }
        if (units == 0) {
            return tens == 8 ? "quatre-vingts" : TENS[tens];
        }
        if (units == 1 && tens != 8) {
            return TENS[tens] + " et un";
        }
        return TENS[tens] + "-" + UNITS[units];
    }

    private SellerInfo resolveSeller() {
        String name = Config.configured(properties, "invoice.seller.name", "org.name");
        String address1 = Config.configured(properties, "invoice.seller.address1", "org.address");
        String address2 = Config.configured(properties, "invoice.seller.address2", null);
        String zip = Config.configured(properties, "invoice.seller.zip", null);
        String city = Config.configured(properties, "invoice.seller.city", null);
        String country = Config.configured(properties, "invoice.seller.country", null);
        String email = Config.configured(properties, "invoice.seller.email", "org.email");
        String phone = Config.configured(properties, "invoice.seller.phone", null);
        String siret = Config.configured(properties, "invoice.seller.siret", null);
        String rna = Config.configured(properties, "invoice.seller.rna", null);
        String prefecture = Config.configured(properties, "invoice.seller.prefecture", null);
        String vatNotice = Config.configured(properties, "invoice.vat.notice", null);
        String footer = Config.configured(properties, "invoice.footer", "org.name");
        return new SellerInfo(name, address1, address2, zip, city, country, email, phone, siret, rna, prefecture, vatNotice, footer);
    }

    private String resolvePaymentDate(Payment payment) {
        if (payment.getUpdatedAt() != null) {
            return payment.getUpdatedAt().toLocalDate().format(DATE_FORMAT);
        }
        if (payment.getCreatedAt() != null) {
            return payment.getCreatedAt().toLocalDate().format(DATE_FORMAT);
        }
        return LocalDateTime.now().format(DATE_FORMAT);
    }

    public String suggestPayerName(
            Payment payment,
            List<PlayerSubscription> subs,
            List<Membership> memberships,
            Find find,
            String fallbackClientName) {
        return resolveClientName(payment, subs, memberships, find, fallbackClientName);
    }

    private String resolveClientName(
            Payment payment,
            List<PlayerSubscription> subs,
            List<Membership> memberships,
            Find find,
            String fallbackClientName) {
        String client = payment.getPayerName();
        if (!isBlank(client)) {
            return client;
        }
        if (fallbackClientName != null && !fallbackClientName.isBlank()) {
            client = fallbackClientName;
        }
        if (!subs.isEmpty() && find != null) {
            IPlayer player = find.player(subs.get(0).getNrFfe(), null);
            String fullName = buildFullName(player);
            if (!fullName.isBlank()) {
                return fullName;
            }
        }
        if (!memberships.isEmpty()) {
            String membershipName = buildMembershipFullName(memberships.get(0));
            if (!membershipName.isBlank()) {
                return membershipName;
            }
        }
        return client == null ? "" : client;
    }

    private String formatAmount(Payment payment) {
        if (payment.getAmountCents() != null) {
            return formatEuros(ServiceUtils.toEuros(payment.getAmountCents()));
        }
        if (payment.getAmount() != null) {
            return formatEuros(payment.getAmount());
        }
        return "-";
    }

    private List<String> buildInvoiceMotifLines(
            List<PlayerSubscription> subs,
            List<Membership> memberships,
            Map<Integer, List<MembershipOptionSubscription>> optionSubscriptions,
            Find find) {
        List<String> lines = new ArrayList<>();
        DateTimeFormatter seasonFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH);

        for (Membership membership : memberships) {
            String memberName = buildMembershipFullName(membership);
            lines.add("Règlement de créance — Adhésion" + (memberName.isBlank() ? "" : " " + memberName));
            List<MembershipOptionSubscription> subscriptions =
                    optionSubscriptions.getOrDefault(membership.getId(), List.of());
            String options = subscriptions.stream()
                    .map(MembershipOptionSubscription::getMembershipOption)
                    .filter(java.util.Objects::nonNull)
                    .map(option -> option.getOptionValue())
                    .filter(value -> value != null && !value.isBlank())
                    .collect(Collectors.joining(", "));
            String license = membership.getLicenseType();
            String detail = joinNotBlank(" — ", isBlank(license) ? "" : "Licence " + license, options);
            if (!isBlank(detail)) {
                lines.add(detail);
            }
            if (membership.getSeason() != null) {
                var season = membership.getSeason();
                if (!isBlank(season.getName())) {
                    String period = "";
                    if (season.getStartDate() != null && season.getEndDate() != null) {
                        period = " du "
                                + season.getStartDate().format(seasonFormatter)
                                + " au "
                                + season.getEndDate().format(seasonFormatter);
                    }
                    lines.add("Pratique sportive du jeu d'échecs" + period + " de la saison " + season.getName());
                }
            }
        }

        for (PlayerSubscription sub : subs) {
            String eventName = sub.getEvent() != null ? sub.getEvent().getName() : "Tournoi";
            String playerInfo = sub.getNrFfe() != null ? sub.getNrFfe() : "";
            if (find != null) {
                IPlayer player = find.player(sub.getNrFfe(), null);
                String fullName = buildFullName(player);
                if (!fullName.isBlank()) {
                    playerInfo = fullName;
                }
            }
            lines.add("Règlement de créance — Inscription " + eventName
                    + (isBlank(playerInfo) ? "" : " — " + playerInfo));
        }

        if (lines.isEmpty()) {
            lines.add("Règlement de créance");
        }
        return lines;
    }

    private Path resolveLogoFile() {
        if (logoService == null || !logoService.exists()) {
            return null;
        }
        return logoService.getLogoFile();
    }

    private void addSignatureImage(Document document) {
        Image image = loadImage(
                signatureService == null || !signatureService.exists() ? null : signatureService.getSignatureFile(),
                170f,
                75f);
        if (image == null) {
            return;
        }
        try {
            image.setAlignment(Element.ALIGN_RIGHT);
            image.setSpacingBefore(6f);
            image.setSpacingAfter(2f);
            document.add(image);
        } catch (DocumentException e) {
            logger.warn("cannot embed invoice signature", e);
        }
    }

    private Image loadImage(Path file, float maxWidth, float maxHeight) {
        if (file == null || !Files.isRegularFile(file)) {
            return null;
        }
        try {
            String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
            if (name.endsWith(".webp")) {
                logger.warn("Invoice image WebP is not embedded in PDF; use PNG or JPEG ({})", name);
                return null;
            }
            Image image = Image.getInstance(Files.readAllBytes(file));
            image.scaleToFit(maxWidth, maxHeight);
            return image;
        } catch (Exception e) {
            logger.warn("cannot load invoice image {}", file, e);
            return null;
        }
    }

    private static void addCenteredLine(
            Document document, String value, Font font, float spacingBefore, float spacingAfter)
            throws DocumentException {
        if (isBlank(value)) {
            return;
        }
        Paragraph paragraph = new Paragraph(value, font);
        paragraph.setAlignment(Element.ALIGN_CENTER);
        paragraph.setSpacingBefore(spacingBefore);
        paragraph.setSpacingAfter(spacingAfter);
        paragraph.setLeading(font.getSize() + 3f);
        document.add(paragraph);
    }

    private static void addLine(Document document, String value, Font font, float spacingBefore, float spacingAfter)
            throws DocumentException {
        if (isBlank(value)) {
            return;
        }
        Paragraph paragraph = new Paragraph(value, font);
        paragraph.setSpacingBefore(spacingBefore);
        paragraph.setSpacingAfter(spacingAfter);
        paragraph.setLeading(font.getSize() + 3f);
        document.add(paragraph);
    }

    private static String formatEuros(double amount) {
        return String.format(Locale.FRANCE, "%.2f €", amount);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String joinNotBlank(String separator, String... values) {
        StringBuilder sb = new StringBuilder();
        for (String value : values) {
            if (isBlank(value)) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(separator);
            }
            sb.append(value.trim());
        }
        return sb.toString();
    }

    private static String buildFullName(IPlayer player) {
        if (player == null) {
            return "";
        }
        String firstname = player.getFirstname() == null ? "" : player.getFirstname().trim();
        String name = player.getName() == null ? "" : player.getName().trim();
        return (firstname + " " + name).trim();
    }

    private static String buildMembershipFullName(Membership membership) {
        if (membership == null) {
            return "";
        }
        String firstname = membership.getFirstname() == null ? "" : membership.getFirstname().trim();
        String lastname = membership.getLastname() == null ? "" : membership.getLastname().trim();
        return (firstname + " " + lastname).trim();
    }

    private record SellerInfo(
            String name,
            String address1,
            String address2,
            String zip,
            String city,
            String country,
            String email,
            String phone,
            String siret,
            String rna,
            String prefecture,
            String vatNotice,
            String footer) {
        String addressLine() {
            return joinNotBlank(
                    " — ",
                    joinNotBlank(", ", address1, address2),
                    joinNotBlank(" ", zip, city),
                    country);
        }

        String contactLine() {
            return joinNotBlank("  ", email, isBlank(phone) ? "" : "Tél. " + phone);
        }

        String legalLine() {
            return joinNotBlank(
                    "  ",
                    isBlank(siret) ? "" : "Siret: " + siret,
                    isBlank(rna) ? "" : "RNA: " + rna);
        }
    }
}

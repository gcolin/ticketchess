package com.github.gcolin.event;

import com.github.gcolin.event.Event;
import com.github.gcolin.event.EventCollection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StatisticsReport {

    private final int total;
    private final double totalAmount;
    private final double totalUnpaidAmount;
    private final Map<String, Double> amountByPaymentType;
    private final Map<String, Integer> byClub;
    private final Map<String, Integer> byCategory;
    private final Map<String, Integer> byFederation;
    private final List<Map<String, Object>> agePyramid;
    private final int agePyramidMax;
    private final List<Map<String, Object>> multiEventPlayers;
    private final List<Map<String, Object>> unknownPaymentPlayers;

    public StatisticsReport(
            int total,
            double totalAmount,
            double totalUnpaidAmount,
            Map<String, Double> amountByPaymentType,
            Map<String, Integer> byClub,
            Map<String, Integer> byCategory,
            Map<String, Integer> byFederation,
            List<Map<String, Object>> agePyramid,
            int agePyramidMax,
            List<Map<String, Object>> multiEventPlayers,
            List<Map<String, Object>> unknownPaymentPlayers) {
        this.total = total;
        this.totalAmount = totalAmount;
        this.totalUnpaidAmount = totalUnpaidAmount;
        this.amountByPaymentType = amountByPaymentType;
        this.byClub = byClub;
        this.byCategory = byCategory;
        this.byFederation = byFederation;
        this.agePyramid = agePyramid;
        this.agePyramidMax = agePyramidMax;
        this.multiEventPlayers = multiEventPlayers;
        this.unknownPaymentPlayers = unknownPaymentPlayers;
    }

    public int getTotal() {
        return total;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public double getTotalUnpaidAmount() {
        return totalUnpaidAmount;
    }

    public Map<String, Double> getAmountByPaymentType() {
        return amountByPaymentType;
    }

    public Map<String, Integer> getByClub() {
        return byClub;
    }

    public Map<String, Integer> getByCategory() {
        return byCategory;
    }

    public Map<String, Integer> getByFederation() {
        return byFederation;
    }

    public List<Map<String, Object>> getAgePyramid() {
        return agePyramid;
    }

    public int getAgePyramidMax() {
        return agePyramidMax;
    }

    public List<Map<String, Object>> getMultiEventPlayers() {
        return multiEventPlayers;
    }

    public List<Map<String, Object>> getUnknownPaymentPlayers() {
        return unknownPaymentPlayers;
    }

    public Map<String, Object> toEventModel(Event event) {
        Map<String, Object> model = new HashMap<>();
        model.put("event", event);
        putCommon(model);
        return model;
    }

    public Map<String, Object> toEventCollectionModel(EventCollection eventCollection) {
        Map<String, Object> model = new HashMap<>();
        model.put("eventCollection", eventCollection);
        putCommon(model);
        model.put("multiEventPlayers", multiEventPlayers);
        return model;
    }

    private void putCommon(Map<String, Object> model) {
        model.put("total", total);
        model.put("totalAmount", totalAmount);
        model.put("totalUnpaidAmount", totalUnpaidAmount);
        model.put("amountByPaymentType", amountByPaymentType);
        model.put("byClub", byClub);
        model.put("byCategory", byCategory);
        model.put("byFederation", byFederation);
        model.put("agePyramid", agePyramid);
        model.put("agePyramidMax", agePyramidMax);
        model.put("unknownPaymentPlayers", unknownPaymentPlayers);
    }
}


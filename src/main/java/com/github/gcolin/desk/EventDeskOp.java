package com.github.gcolin.desk;

import java.util.ArrayList;
import java.util.List;

public class EventDeskOp {

    private String id;
    private Integer subId;
    private Integer optionId;
    private Boolean present;
    private String status;
    private Long clientTs;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Integer getSubId() {
        return subId;
    }

    public void setSubId(Integer subId) {
        this.subId = subId;
    }

    public Integer getOptionId() {
        return optionId;
    }

    public void setOptionId(Integer optionId) {
        this.optionId = optionId;
    }

    public Boolean getPresent() {
        return present;
    }

    public void setPresent(Boolean present) {
        this.present = present;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getClientTs() {
        return clientTs;
    }

    public void setClientTs(Long clientTs) {
        this.clientTs = clientTs;
    }

    public static class SyncBatch {
        private Integer eventId;
        private List<EventDeskOp> ops = new ArrayList<>();

        public Integer getEventId() {
            return eventId;
        }

        public void setEventId(Integer eventId) {
            this.eventId = eventId;
        }

        public List<EventDeskOp> getOps() {
            return ops;
        }

        public void setOps(List<EventDeskOp> ops) {
            this.ops = ops == null ? new ArrayList<>() : ops;
        }
    }

    public static class SyncRequest {
        private String type;
        private Integer eventId;
        private List<EventDeskOp> ops = new ArrayList<>();
        private List<SyncBatch> batches = new ArrayList<>();
        private List<Integer> eventIds = new ArrayList<>();

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Integer getEventId() {
            return eventId;
        }

        public void setEventId(Integer eventId) {
            this.eventId = eventId;
        }

        public List<EventDeskOp> getOps() {
            return ops;
        }

        public void setOps(List<EventDeskOp> ops) {
            this.ops = ops == null ? new ArrayList<>() : ops;
        }

        public List<SyncBatch> getBatches() {
            return batches;
        }

        public void setBatches(List<SyncBatch> batches) {
            this.batches = batches == null ? new ArrayList<>() : batches;
        }

        public List<Integer> getEventIds() {
            return eventIds;
        }

        public void setEventIds(List<Integer> eventIds) {
            this.eventIds = eventIds == null ? new ArrayList<>() : eventIds;
        }
    }
}

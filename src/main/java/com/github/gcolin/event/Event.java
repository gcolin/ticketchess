package com.github.gcolin.event;

import com.github.gcolin.player.DisplayPlayer;
import com.github.gcolin.event.EventStatus;
import com.github.gcolin.event.EventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Index;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlTransient;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.github.gcolin.registration.PlayerSubscription;

@Entity
@Table(
        name = "event",
        indexes = {
            @Index(name = "idx_event_status_id", columnList = "status_id"),
            @Index(name = "idx_event_start_date", columnList = "start_date")
        })
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDateTime startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDateTime endDate;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "pricecents")
    private Long priceCents;

    @Column(name = "youngPrice", precision = 10, scale = 2)
    private BigDecimal youngPrice;

    @Column(name = "youngpricecents")
    private Long youngPriceCents;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status_id", nullable = false)
    private EventStatus status = EventStatus.DRAFT;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "event_type", nullable = false)
    private EventType eventType = EventType.STANDARD;

    @OneToMany(mappedBy = "event")
    @XmlTransient
    private List<PlayerSubscription> subscriptions = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_group_id")
    @XmlTransient
    private EventGroup eventGroup;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "big_event_id")
    @XmlTransient
    private EventCollection eventCollection;

    @OneToOne(fetch = FetchType.LAZY, mappedBy = "event")
    @XmlTransient
    private EventInfo eventInfo;

    @Transient
    private Map<EventOptionType, EventOption> eventOptions;

    @XmlTransient
    @Transient
    private List<DisplayPlayer> players;

    @XmlTransient
    @Transient
    private Integer nbSubscriptions;
    public Event() {}

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public BigDecimal getPrice() {
        if (price == null && priceCents != null) {
            price = fromCents(priceCents);
        }
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
        this.priceCents = toCents(price);
    }

    public BigDecimal getYoungPrice() {
        if (youngPrice == null && youngPriceCents != null) {
            youngPrice = fromCents(youngPriceCents);
        }
        return youngPrice;
    }

    public void setYoungPrice(BigDecimal youngPrice) {
        this.youngPrice = youngPrice;
        this.youngPriceCents = toCents(youngPrice);
    }

    public long getPriceCents() {
        return priceCents == null ? 0L : priceCents;
    }

    public void setPriceCents(Long priceCents) {
        this.priceCents = priceCents;
        this.price = fromCents(priceCents);
    }

    public long getYoungPriceCents() {
        return youngPriceCents == null ? 0L : youngPriceCents;
    }

    public void setYoungPriceCents(Long youngPriceCents) {
        this.youngPriceCents = youngPriceCents;
        this.youngPrice = fromCents(youngPriceCents);
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public EventType getEventType() {
        return eventType;
    }

    public void setEventType(EventType eventType) {
        this.eventType = eventType;
    }

    public List<PlayerSubscription> getSubscriptions() {
        return subscriptions;
    }

    public void setSubscriptions(List<PlayerSubscription> subscriptions) {
        this.subscriptions = subscriptions;
    }

    public List<DisplayPlayer> getPlayers() {
        return players;
    }

    public void setPlayers(List<DisplayPlayer> players) {
        this.players = players;
    }

    public Integer getNbSubscriptions() {
        return nbSubscriptions;
    }

    public void setNbSubscriptions(Integer nbSubscriptions) {
        this.nbSubscriptions = nbSubscriptions;
    }

    public Date getStartDateAsDate() {
        if (startDate == null) {
            return null;
        }
        return Date.from(startDate.atZone(ZoneId.systemDefault()).toInstant());
    }

    public Date getEndDateAsDate() {
        if (endDate == null) {
            return null;
        }
        return Date.from(endDate.atZone(ZoneId.systemDefault()).toInstant());
    }

    public void setEventGroup(EventGroup eventGroup) {
        this.eventGroup = eventGroup;
    }

    public EventGroup getEventGroup() {
        return eventGroup;
    }

    public boolean isFree() {
        return getPriceCents() == 0L && getYoungPriceCents() == 0L;
    }

    @PostLoad
    public void fillCentsFromLegacyValues() {
        if (priceCents == null && price != null) {
            priceCents = toCents(price);
        }
        if (youngPriceCents == null && youngPrice != null) {
            youngPriceCents = toCents(youngPrice);
        }
    }

    @PrePersist
    @PreUpdate
    public void syncLegacyAndCentsValues() {
        if (priceCents == null && price != null) {
            priceCents = toCents(price);
        }
        if (price == null && priceCents != null) {
            price = fromCents(priceCents);
        }

        if (youngPriceCents == null && youngPrice != null) {
            youngPriceCents = toCents(youngPrice);
        }
        if (youngPrice == null && youngPriceCents != null) {
            youngPrice = fromCents(youngPriceCents);
        }
    }

    private static Long toCents(BigDecimal value) {
        if (value == null) {
            return null;
        }
        return value.movePointRight(2).setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private static BigDecimal fromCents(Long cents) {
        if (cents == null) {
            return null;
        }
        return BigDecimal.valueOf(cents, 2);
    }

    public Integer getMaxSubscriptions() {
        if (eventOptions == null || !eventOptions.containsKey(EventOptionType.MAX_SUBSCRIPTIONS)) {
            return null;
        }
        EventOption option = eventOptions.get(EventOptionType.MAX_SUBSCRIPTIONS);
        if (option == null || option.getValue() == null || option.getValue().isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(option.getValue());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public String getFfeId() {
        if (eventOptions == null || !eventOptions.containsKey(EventOptionType.FFE_ID)) {
            return null;
        }
        EventOption option = eventOptions.get(EventOptionType.FFE_ID);
        if (option == null || option.getValue() == null || option.getValue().isBlank()) {
            return null;
        }
        return option.getValue().trim();
    }

    public boolean isPointageEnabled() {
        if (eventOptions == null || !eventOptions.containsKey(EventOptionType.POINTAGE)) {
            return false;
        }
        EventOption option = eventOptions.get(EventOptionType.POINTAGE);
        return option != null && "1".equals(option.getValue());
    }

    public Map<EventOptionType, EventOption> getEventOptions() {
        return eventOptions;
    }

    public void setEventOptions(Map<EventOptionType, EventOption> eventOptions) {
        this.eventOptions = eventOptions;
    }

    public EventInfo getEventInfo() {
        return eventInfo;
    }

    public void setEventInfo(EventInfo eventInfo) {
        this.eventInfo = eventInfo;
    }

    public EventCollection getEventCollection() {
        return eventCollection;
    }

    public void setEventCollection(EventCollection eventCollection) {
        this.eventCollection = eventCollection;
    }

    @Override
    public String toString() {
        return "Event [id=" + id + ", name=" + name + "]";
    }
}

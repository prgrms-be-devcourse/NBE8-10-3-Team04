package com.back.domain.item.itemHistory.entity;

import com.back.domain.item.item.entity.Item;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "item_histories") // 테이블명
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@AllArgsConstructor
@Builder
public class ItemHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) // item(1) : item_history(N)
    private Item item;

    private LocalDate startDate;
    private LocalDate endDate;

    public ItemHistory(Item item) {
        this.item = item;
        this.startDate = item.getStartDate();
        this.endDate = null;
    }

    public void end(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Long getUsedDays() {
        if (endDate == null || startDate == null) return null;
        long days = ChronoUnit.DAYS.between(startDate, endDate);
        return Math.max(days, 0);
    }
}

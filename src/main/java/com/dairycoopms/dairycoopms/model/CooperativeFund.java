package com.dairycoopms.dairycoopms.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cooperative_fund", schema = "dairycoopms")
public class CooperativeFund {

    @Id
    @Column(name = "id", columnDefinition = "TINYINT")
    private int id = 1; // always 1 — single row

    @Column(name = "balance", nullable = false, precision = 14, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "last_topped_up")
    private LocalDateTime lastToppedUp;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topped_up_by")
    private User toppedUpBy;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public int          getId()           { return id; }
    public BigDecimal   getBalance()      { return balance; }
    public LocalDateTime getLastToppedUp(){ return lastToppedUp; }
    public User          getToppedUpBy()  { return toppedUpBy; }
    public String        getNotes()       { return notes; }
    public LocalDateTime getUpdatedAt()   { return updatedAt; }

    public void setBalance(BigDecimal balance)          { this.balance = balance; }
    public void setLastToppedUp(LocalDateTime t)        { this.lastToppedUp = t; }
    public void setToppedUpBy(User u)                  { this.toppedUpBy = u; }
    public void setNotes(String n)                     { this.notes = n; }
    public void setUpdatedAt(LocalDateTime t)          { this.updatedAt = t; }

    @PrePersist @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public boolean hasSufficientFunds(BigDecimal amount) {
        return balance != null && balance.compareTo(amount) >= 0;
    }
}
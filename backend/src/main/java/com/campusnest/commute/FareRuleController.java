package com.campusnest.commute;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.campusnest.common.error.ApiException;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

/**
 * Fare rules are public (so students can see where cost estimates come from) and maintained by administrators.
 * Editing = remove the old rule and add the new one, which keeps every rule's source explicit.
 */
@RestController
public class FareRuleController {

    private final FareRuleRepository rules;

    public FareRuleController(FareRuleRepository rules) {
        this.rules = rules;
    }

    @GetMapping("/api/v1/fare-rules")
    @Transactional(readOnly = true)
    public List<FareRuleView> list() {
        return rules.findAllByOrderByModeAscUpToKmAsc().stream().map(FareRuleView::from).toList();
    }

    @PostMapping("/api/v1/admin/fare-rules")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public FareRuleView add(@Valid @RequestBody FareRuleRequest r) {
        boolean metered = r.kind() == FareRule.Kind.PER_TRIP_METERED;
        if (metered && (r.baseKm() == null || r.perKm() == null)) {
            throw ApiException.badRequest("INVALID_FARE_RULE", "A metered fare needs the distance covered by the minimum fare and a per-km rate.");
        }
        if (!metered && r.upToKm() == null) {
            throw ApiException.badRequest("INVALID_FARE_RULE", "A slab fare needs the 'up to km' distance.");
        }
        boolean modeMatches = switch (r.kind()) {
            case PER_TRIP_METERED -> r.mode() == TravelMode.AUTO_RICKSHAW;
            case PER_TRIP_SLAB -> r.mode() == TravelMode.BUS;
            case MONTHLY_PASS_SLAB -> r.mode() == TravelMode.LOCAL_TRAIN;
        };
        if (!modeMatches) {
            throw ApiException.badRequest("INVALID_FARE_RULE", "Use metered fares for auto-rickshaws, trip slabs for buses and monthly pass slabs for trains.");
        }
        FareRule saved = rules.save(new FareRule(r.mode(), r.kind(), metered ? null : r.upToKm(), r.baseFare(),
                metered ? r.baseKm() : null, metered ? r.perKm() : null, r.description().trim(), r.sourceName().trim(),
                r.sourceUrl() == null || r.sourceUrl().isBlank() ? null : r.sourceUrl().trim(), r.effectiveFrom()));
        return FareRuleView.from(saved);
    }

    @DeleteMapping("/api/v1/admin/fare-rules/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void remove(@PathVariable Long id) {
        if (!rules.existsById(id)) {
            throw ApiException.notFound("FARE_RULE_NOT_FOUND", "Fare rule not found.");
        }
        rules.deleteById(id);
    }

    /** A source is mandatory: CampusNest only uses fares that can be traced back to where they came from. */
    public record FareRuleRequest(
            @NotNull TravelMode mode,
            @NotNull FareRule.Kind kind,
            @DecimalMin("0.1") @DecimalMax("500") BigDecimal upToKm,
            @NotNull @DecimalMin("0") @DecimalMax("100000") BigDecimal baseFare,
            @DecimalMin("0") @DecimalMax("50") BigDecimal baseKm,
            @DecimalMin("0") @DecimalMax("1000") BigDecimal perKm,
            @NotBlank @Size(max = 200) String description,
            @NotBlank @Size(max = 200) String sourceName,
            @Size(max = 500) String sourceUrl,
            @NotNull @PastOrPresent LocalDate effectiveFrom) {
    }

    public record FareRuleView(Long id, TravelMode mode, FareRule.Kind kind, BigDecimal upToKm, BigDecimal baseFare,
            BigDecimal baseKm, BigDecimal perKm, String description, String sourceName, String sourceUrl,
            LocalDate effectiveFrom) {

        static FareRuleView from(FareRule f) {
            return new FareRuleView(f.getId(), f.getMode(), f.getKind(), f.getUpToKm(), f.getBaseFare(), f.getBaseKm(),
                    f.getPerKm(), f.getDescription(), f.getSourceName(), f.getSourceUrl(), f.getEffectiveFrom());
        }
    }
}

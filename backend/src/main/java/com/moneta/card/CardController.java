package com.moneta.card;

import com.moneta.card.CardDtos.CardLimitSummary;
import com.moneta.card.CardDtos.CardResponse;
import com.moneta.card.CardDtos.CreateCardRequest;
import com.moneta.card.CardDtos.UpdateCardRequest;
import com.moneta.card.CardInvoiceDtos.CardInvoiceResponse;
import com.moneta.config.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cards")
@Validated
public class CardController {
  private final CardService cardService;
  private final CardInvoiceService cardInvoiceService;

  public CardController(CardService cardService, CardInvoiceService cardInvoiceService) {
    this.cardService = cardService;
    this.cardInvoiceService = cardInvoiceService;
  }

  @GetMapping
  public List<CardResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
    return cardService.list(principal.getId()).stream()
      .map(this::toResponse)
      .toList();
  }

  @PostMapping
  public CardResponse create(
    @AuthenticationPrincipal UserPrincipal principal,
    @Valid @RequestBody CreateCardRequest request
  ) {
    Card card = cardService.create(principal.getId(), request);
    return toResponse(card);
  }

  @GetMapping("/{id}")
  public CardResponse get(
    @AuthenticationPrincipal UserPrincipal principal,
    @PathVariable Long id
  ) {
    Card card = cardService.get(principal.getId(), id);
    return toResponse(card);
  }

  @PatchMapping("/{id}")
  public CardResponse update(
    @AuthenticationPrincipal UserPrincipal principal,
    @PathVariable Long id,
    @Valid @RequestBody UpdateCardRequest request
  ) {
    Card card = cardService.update(principal.getId(), id, request);
    return toResponse(card);
  }

  @DeleteMapping("/{id}")
  public void delete(
    @AuthenticationPrincipal UserPrincipal principal,
    @PathVariable Long id
  ) {
    cardService.disable(principal.getId(), id);
  }

  @GetMapping("/{id}/invoice")
  public CardInvoiceResponse getInvoice(
    @AuthenticationPrincipal UserPrincipal principal,
    @PathVariable Long id,
    @RequestParam @Min(value = 1900, message = "ano deve estar entre 1900 e 2100") @Max(value = 2100, message = "ano deve estar entre 1900 e 2100") int year,
    @RequestParam @Min(value = 1, message = "mês deve estar entre 1 e 12") @Max(value = 12, message = "mês deve estar entre 1 e 12") int month
  ) {
    return cardInvoiceService.getInvoice(principal.getId(), id, year, month);
  }

  @GetMapping("/limit-summary")
  public List<CardLimitSummary> getLimitSummary(
    @AuthenticationPrincipal UserPrincipal principal,
    @RequestParam(required = false) String asOf
  ) {
    LocalDate asOfDate = asOf != null ? LocalDate.parse(asOf) : null;
    return cardService.getLimitSummary(principal.getId(), asOfDate);
  }

  private CardResponse toResponse(Card card) {
    return new CardResponse(
      card.getId(),
      card.getAccount().getId(),
      card.getAccount().getName(),
      card.getName(),
      card.getBrand(),
      card.getLast4(),
      card.getLimitAmount(),
      card.getClosingDay(),
      card.getDueDay(),
      card.isActive(),
      card.getCreatedAt()
    );
  }
}

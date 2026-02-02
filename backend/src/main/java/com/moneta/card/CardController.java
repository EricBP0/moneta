package com.moneta.card;

import com.moneta.card.CardDtos.CardInvoiceResponse;
import com.moneta.card.CardDtos.CardResponse;
import com.moneta.card.CardDtos.CreateCardRequest;
import com.moneta.card.CardDtos.UpdateCardRequest;
import com.moneta.config.UserPrincipal;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cards")
public class CardController {
  private final CardService cardService;

  public CardController(CardService cardService) {
    this.cardService = cardService;
  }

  @PostMapping
  public CardResponse create(
    @AuthenticationPrincipal UserPrincipal principal,
    @Valid @RequestBody CreateCardRequest request
  ) {
    return toResponse(cardService.createCard(principal.getId(), request));
  }

  @GetMapping
  public List<CardResponse> list(@AuthenticationPrincipal UserPrincipal principal) {
    return cardService.listCards(principal.getId()).stream()
      .map(this::toResponse)
      .toList();
  }

  @GetMapping("/{id}")
  public CardResponse get(
    @AuthenticationPrincipal UserPrincipal principal,
    @PathVariable Long id
  ) {
    return toResponse(cardService.getCard(principal.getId(), id));
  }

  @PutMapping("/{id}")
  public CardResponse update(
    @AuthenticationPrincipal UserPrincipal principal,
    @PathVariable Long id,
    @Valid @RequestBody UpdateCardRequest request
  ) {
    return toResponse(cardService.updateCard(principal.getId(), id, request));
  }

  @DeleteMapping("/{id}")
  public void delete(
    @AuthenticationPrincipal UserPrincipal principal,
    @PathVariable Long id
  ) {
    cardService.disableCard(principal.getId(), id);
  }

  @GetMapping("/{id}/invoice")
  public CardInvoiceResponse invoice(
    @AuthenticationPrincipal UserPrincipal principal,
    @PathVariable Long id,
    @RequestParam int year,
    @RequestParam int month
  ) {
    return cardService.getInvoice(principal.getId(), id, year, month);
  }

  private CardResponse toResponse(Card card) {
    return new CardResponse(
      card.getId(),
      card.getAccount().getId(),
      card.getName(),
      card.getBrand(),
      card.getLast4(),
      card.getLimitAmount(),
      card.getClosingDay(),
      card.getDueDay(),
      card.isActive()
    );
  }
}

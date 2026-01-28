package com.moneta.rule;

import com.moneta.txn.Txn;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RuleService {
  public List<Txn> applyRules(Long userId, List<Txn> txns) {
    return Collections.emptyList();
  }
}

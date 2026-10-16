#pragma once

#include <stdexcept>
#include <string>
#include <vector>
#include "sentencepiece_processor.h"

// Canonical XLMRobertaTokenizer (Hugging Face use_fast=false): upstream
// SentencePiece normalization/segmentation, special-token splitting and fairseq ids.
// Do not approximate Unicode normalization, Unigram scores or the 250k-piece vocabulary.
class E5Tokenizer {
 public:
  explicit E5Tokenizer(const std::string &path) {
    check(processor_.Load(path));
    if (processor_.GetPieceSize() != 250000 || processor_.unk_id() != 0)
      throw std::runtime_error("Tokenizador E5 incompatible");
  }
  std::vector<int> encode(const std::string &text, int max_tokens) const {
    if (max_tokens < 2 || max_tokens > 512 || text.size() > 131072)
      throw std::runtime_error("Entrada fuera del presupuesto E5");
    std::vector<int> result{0};
    const std::string specials[] = {"<s>", "<pad>", "</s>", "<unk>", "<mask>"};
    const int ids[] = {0, 1, 2, 3, 250001};
    size_t cursor = 0;
    while (cursor < text.size() && result.size() < static_cast<size_t>(max_tokens - 1)) {
      size_t next = text.size();
      int special = -1;
      for (int i = 0; i < 5; ++i) {
        auto found = text.find(specials[i], cursor);
        if (found < next) { next = found; special = i; }
      }
      std::vector<int> pieces;
      check(processor_.Encode(text.substr(cursor, next - cursor), &pieces));
      for (int id : pieces) {
        if (result.size() >= static_cast<size_t>(max_tokens - 1)) break;
        result.push_back(id == 0 ? 3 : id + 1);
      }
      if (special < 0) break;
      if (result.size() < static_cast<size_t>(max_tokens - 1)) result.push_back(ids[special]);
      cursor = next + specials[special].size();
    }
    result.push_back(2);
    return result;
  }
 private:
  static void check(const sentencepiece::util::Status &status) {
    if (!status.ok()) throw std::runtime_error(status.ToString());
  }
  sentencepiece::SentencePieceProcessor processor_;
};

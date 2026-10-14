// Tests the same pinned llama.cpp grammar engine without downloading model weights.
#include "llama-grammar.h"
#include "unicode.h"
#include <fstream>
#include <iostream>
#include <iterator>
#include <memory>
#include <stdexcept>
#include <string>

static std::string read(const char *path) {
    std::ifstream file(path, std::ios::binary);
    if (!file) throw std::runtime_error("Missing input file");
    return std::string(std::istreambuf_iterator<char>(file), std::istreambuf_iterator<char>());
}
int main(int argc, char **argv) {
    if (argc != 3) return 2;
    try {
        const auto rule = read(argv[1]);
        std::unique_ptr<llama_grammar, decltype(&llama_grammar_free_impl)> grammar(
            llama_grammar_init_impl(nullptr, rule.c_str(), "root", false, nullptr, 0, nullptr, 0),
            llama_grammar_free_impl);
        if (!grammar) return 2;
        for (auto point : unicode_cpts_from_utf8(read(argv[2]))) {
            llama_grammar_accept(grammar.get(), point);
            if (llama_grammar_get_stacks(grammar.get()).empty()) return 1;
        }
        for (const auto &stack : llama_grammar_get_stacks(grammar.get())) if (stack.empty()) return 0;
        return 1; // truncated output is not a complete document
    } catch (const std::exception &error) { std::cerr << error.what() << '\n'; return 2; }
}

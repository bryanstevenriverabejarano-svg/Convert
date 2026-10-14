#!/usr/bin/env python3
"""Entrena un adaptador LoRA personal para Salve en un PC con GPU.

Requisitos (no se instalan en CI): torch, transformers, peft, datasets, trl.
  pip install torch transformers peft datasets trl

El modelo base de Hugging Face debe ser EXACTAMENTE el que originó el GGUF del móvil
(por ejemplo cognitivecomputations/Dolphin3.0-Llama3.2-3B para Dolphin3.0-Llama3.2-3B-Q4_K_M.gguf).
Después se convierte con llama.cpp:
  python third_party/llama.cpp/convert_lora_to_gguf.py --base <dir_modelo_hf> --outfile salve-lora.gguf <dir_adaptador>
y se describe con make_manifest.py.
"""
import argparse


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--base", required=True, help="Modelo base de Hugging Face (ruta o id)")
    parser.add_argument("--data", required=True, help="train.jsonl de prepare_dataset.py")
    parser.add_argument("--output", required=True, help="Directorio del adaptador")
    parser.add_argument("--rank", type=int, default=16)
    parser.add_argument("--epochs", type=float, default=2.0)
    parser.add_argument("--lr", type=float, default=1e-4)
    args = parser.parse_args()

    from datasets import load_dataset
    from peft import LoraConfig
    from transformers import AutoModelForCausalLM, AutoTokenizer
    from trl import SFTConfig, SFTTrainer

    tokenizer = AutoTokenizer.from_pretrained(args.base)
    model = AutoModelForCausalLM.from_pretrained(args.base, torch_dtype="auto", device_map="auto")
    dataset = load_dataset("json", data_files=args.data, split="train")
    lora = LoraConfig(r=args.rank, lora_alpha=args.rank * 2, lora_dropout=0.05, task_type="CAUSAL_LM",
                      target_modules=["q_proj", "k_proj", "v_proj", "o_proj"])
    trainer = SFTTrainer(
        model=model,
        processing_class=tokenizer,
        train_dataset=dataset,
        peft_config=lora,
        args=SFTConfig(output_dir=args.output, num_train_epochs=args.epochs, learning_rate=args.lr,
                       per_device_train_batch_size=2, gradient_accumulation_steps=4, logging_steps=5,
                       save_strategy="no", report_to=[]),
    )
    trainer.train()
    trainer.model.save_pretrained(args.output)
    print(f"Adaptador guardado en {args.output}. Conviértelo a GGUF con llama.cpp (ver la ayuda).")


if __name__ == "__main__":
    main()

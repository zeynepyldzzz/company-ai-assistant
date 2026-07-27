import { Plus, Trash2 } from "lucide-react";
import type { ProcedureStep } from "@company/shared";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";

// Formdaki tek adim satiri. order gonderirken otomatik (sira) atanir, burada tutulmaz.
export type StepDraft = { title: string; detail: string };

// Bos satirlari eler, kalanlara 1'den order verir. Hem create hem yeni-versiyon formu kullanir.
export function toProcedureSteps(steps: StepDraft[]): ProcedureStep[] {
  return steps
    .filter((s) => s.title.trim() || s.detail.trim())
    .map((s, i) => ({ order: i + 1, title: s.title.trim(), detail: s.detail.trim() }));
}

export function StepsEditor({
  steps,
  onChange,
}: {
  steps: StepDraft[];
  onChange: (steps: StepDraft[]) => void;
}) {
  function updateStep(index: number, patch: Partial<StepDraft>) {
    onChange(steps.map((s, i) => (i === index ? { ...s, ...patch } : s)));
  }

  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between">
        <Label>Adımlar</Label>
        <Button
          type="button"
          variant="outline"
          size="sm"
          onClick={() => onChange([...steps, { title: "", detail: "" }])}
        >
          <Plus />
          Adım Ekle
        </Button>
      </div>
      {steps.length === 0 && (
        <p className="text-muted-foreground text-sm">Adım eklenmedi (opsiyonel).</p>
      )}
      {steps.map((step, index) => (
        <div key={index} className="flex items-start gap-2 rounded-lg border p-2">
          <span className="text-muted-foreground pt-2 text-sm font-medium">{index + 1}.</span>
          <div className="flex-1 space-y-1.5">
            <Input
              value={step.title}
              onChange={(event) => updateStep(index, { title: event.target.value })}
              placeholder="Adım başlığı"
            />
            <Input
              value={step.detail}
              onChange={(event) => updateStep(index, { detail: event.target.value })}
              placeholder="Adım detayı"
            />
          </div>
          <Button
            type="button"
            variant="ghost"
            size="icon-sm"
            onClick={() => onChange(steps.filter((_, i) => i !== index))}
          >
            <Trash2 />
          </Button>
        </div>
      ))}
    </div>
  );
}

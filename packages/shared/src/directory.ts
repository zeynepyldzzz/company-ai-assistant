import { z } from "zod";

// docs/apiEndpoints.md #0 Genel Kurallar: liste donen endpoint'ler bu zarfi kullanir.
export function pagedResponseSchema<T extends z.ZodTypeAny>(item: T) {
  return z.object({
    data: z.array(item),
    page: z.number(),
    pageSize: z.number(),
    total: z.number(),
  });
}

// Backend'de office_status serbest metin (enum yok); B-4 icin sabitlenen 3 deger.
export const OfficeStatusSchema = z.enum(["Ofiste", "Uzaktan", "Izinde"]);
export type OfficeStatus = z.infer<typeof OfficeStatusSchema>;

// GET /employees, GET /employees/{id}
export const EmployeeSchema = z.object({
  id: z.number(),
  name: z.string(),
  email: z.string().email(),
  phone: z.string().nullable(),
  officeStatus: z.string().nullable(),
  departmentId: z.number().nullable(),
  departmentName: z.string().nullable(),
});
export type Employee = z.infer<typeof EmployeeSchema>;
export const EmployeePagedResponseSchema = pagedResponseSchema(EmployeeSchema);
export type EmployeePagedResponse = z.infer<typeof EmployeePagedResponseSchema>;

// GET /departments, GET /departments/{id}
export const DepartmentSchema = z.object({
  id: z.number(),
  name: z.string(),
  responsibilities: z.string().nullable(),
  managerId: z.number().nullable(),
  managerName: z.string().nullable(),
  managerEmail: z.string().nullable(),
  managerPhone: z.string().nullable(),
});
export type Department = z.infer<typeof DepartmentSchema>;
export const DepartmentPagedResponseSchema = pagedResponseSchema(DepartmentSchema);
export type DepartmentPagedResponse = z.infer<typeof DepartmentPagedResponseSchema>;

// GET /phonebook
export const PhonebookEntrySchema = z.object({
  id: z.number(),
  name: z.string(),
  extension: z.string().nullable(),
  departmentName: z.string().nullable(),
});
export type PhonebookEntry = z.infer<typeof PhonebookEntrySchema>;
export const PhonebookEntryPagedResponseSchema = pagedResponseSchema(PhonebookEntrySchema);
export type PhonebookEntryPagedResponse = z.infer<typeof PhonebookEntryPagedResponseSchema>;

// POST /phonebook/{extension}/call
export const CallTriggerResponseSchema = z.object({
  extension: z.string(),
  status: z.string(),
  triggeredAt: z.string(),
});
export type CallTriggerResponse = z.infer<typeof CallTriggerResponseSchema>;

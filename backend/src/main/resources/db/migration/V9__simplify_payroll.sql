-- Simplify salary structures: add annual_ctc as the single input field
-- and the new component columns (da, conveyance, pf_applicable, esi_applicable).
-- Existing columns are kept untouched; pf/esi fields will be written as 0 going forward.
ALTER TABLE salary_structures
    ADD COLUMN annual_ctc   DECIMAL(14,2) NOT NULL DEFAULT 0,
    ADD COLUMN da           DECIMAL(14,2) NOT NULL DEFAULT 0,
    ADD COLUMN conveyance   DECIMAL(14,2) NOT NULL DEFAULT 0,
    ADD COLUMN pf_applicable  BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN esi_applicable BOOLEAN NOT NULL DEFAULT FALSE;

-- Fix 12: rename payslip columns — lta was storing DA, transport_allowance was storing conveyance
ALTER TABLE payslips RENAME COLUMN lta               TO da;
ALTER TABLE payslips RENAME COLUMN transport_allowance TO conveyance;

-- Fix 1 & 14: add disbursement support and processedAt audit stamp to payroll_runs
ALTER TABLE payroll_runs
    ADD COLUMN processed_at  TIMESTAMP WITH TIME ZONE,
    ADD COLUMN disbursed_by  UUID REFERENCES users(id) ON DELETE SET NULL,
    ADD COLUMN disbursed_at  TIMESTAMP WITH TIME ZONE;

-- Fix 7: seed missing earning components (DA and CONVEYANCE) so buildStructure() can read them
INSERT INTO payroll_components (name, code, component_type, calculation_type, value, percentage_of,
                                is_taxable, display_order, created_by, updated_by)
VALUES
    ('Dearness Allowance',   'DA',         'EARNING', 'PERCENTAGE', 10.00, 'CTC', TRUE, 25, 'system', 'system'),
    ('Conveyance Allowance', 'CONVEYANCE', 'EARNING', 'PERCENTAGE', 10.00, 'CTC', TRUE, 26, 'system', 'system')
ON CONFLICT (code) DO NOTHING;

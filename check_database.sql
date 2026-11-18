-- Run this in your MySQL/MariaDB database to check the schema

-- 1. Check if loans table exists and see its structure
DESCRIBE dairycoopms.loans;

-- 2. Check current loans and their disbursement status
SELECT
    id,
    loan_amount,
    status,
    disbursement_status,
    disbursed_at,
    disbursement_reference
FROM dairycoopms.loans
WHERE status = 'APPROVED'
ORDER BY request_date DESC;

-- 3. Check if disbursement_status column exists
SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE
FROM INFORMATION_SCHEMA.COLUMNS
WHERE TABLE_SCHEMA = 'dairycoopms'
  AND TABLE_NAME = 'loans'
  AND COLUMN_NAME LIKE '%disburs%';

-- 4. If disbursement columns are MISSING, run this to add them:
-- (ONLY run if columns don't exist!)
/*
ALTER TABLE dairycoopms.loans
ADD COLUMN IF NOT EXISTS disbursement_status VARCHAR(30) NULL AFTER status;

ALTER TABLE dairycoopms.loans
ADD COLUMN IF NOT EXISTS disbursed_at DATETIME NULL AFTER disbursement_status;

ALTER TABLE dairycoopms.loans
ADD COLUMN IF NOT EXISTS disbursed_by BINARY(16) NULL AFTER disbursed_at;

ALTER TABLE dairycoopms.loans
ADD COLUMN IF NOT EXISTS disbursement_reference VARCHAR(100) NULL AFTER disbursed_by;

ALTER TABLE dairycoopms.loans
ADD COLUMN IF NOT EXISTS disbursement_notes TEXT NULL AFTER disbursement_reference;

-- Add foreign key for disbursed_by
ALTER TABLE dairycoopms.loans
ADD CONSTRAINT fk_loan_disbursed_by
FOREIGN KEY (disbursed_by) REFERENCES dairycoopms.users(id);
*/

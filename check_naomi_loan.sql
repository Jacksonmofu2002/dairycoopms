-- Check NAOMI KAFWIMBI's loan details

-- 1. Find all loans for member with 'NAOMI' in name
SELECT
    l.id,
    m.name as member_name,
    m.member_number,
    l.loan_amount,
    l.status,
    l.disbursement_status,
    l.disbursed_at,
    l.disbursement_reference,
    l.approved_date,
    l.request_date
FROM dairycoopms.loans l
JOIN dairycoopms.members m ON l.member_id = m.id
WHERE m.name LIKE '%NAOMI%'
ORDER BY l.request_date DESC;

-- 2. Check if there's a COFA record
SELECT * FROM dairycoopms.cooperative_fund;

-- 3. Check recent COFA transactions
SELECT
    transaction_type,
    amount,
    balance_after,
    reference_no,
    description,
    created_at
FROM dairycoopms.cooperative_fund_transactions
ORDER BY created_at DESC
LIMIT 10;

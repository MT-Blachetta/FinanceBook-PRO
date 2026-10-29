-- FinanceBook V2: integrity constraints for user data and transaction fees.
--
-- The Java backend and the Python admin panel both write to these tables, so
-- the rules both of them rely on are enforced by the database itself.

-- ─── Users ──────────────────────────────────────────────────────────

-- Usernames and names must not be blank (both applications normalize whitespace).
ALTER TABLE "user"
    ADD CONSTRAINT user_username_not_blank CHECK (btrim(username) <> ''),
    ADD CONSTRAINT user_names_not_blank CHECK (btrim(surname) <> '' AND btrim(prename) <> '');

-- Redundant: the UNIQUE constraint on username already provides an index.
DROP INDEX IF EXISTS idx_user_username;

-- ─── Fee plans ──────────────────────────────────────────────────────

ALTER TABLE transactionfeeplan
    ADD CONSTRAINT transactionfeeplan_mode_check CHECK (mode IN ('table', 'formula'));

-- Redundant: the UNIQUE constraint on user_id already provides an index.
DROP INDEX IF EXISTS idx_feeplan_user;

-- ─── Fee records ────────────────────────────────────────────────────

-- A payment item carries at most one fee record. Remove any duplicates
-- (keeping the newest) before adding the constraint.
DELETE FROM transactionfeerecord older
    USING transactionfeerecord newer
    WHERE older.payment_item_id = newer.payment_item_id
      AND older.id < newer.id;

ALTER TABLE transactionfeerecord
    ADD CONSTRAINT transactionfeerecord_payment_item_id_key UNIQUE (payment_item_id);

-- Redundant: the UNIQUE constraint above provides the index.
DROP INDEX IF EXISTS idx_feerecord_payment;

-- A fee record is removed together with its payment item.
ALTER TABLE transactionfeerecord
    DROP CONSTRAINT transactionfeerecord_payment_item_id_fkey,
    ADD CONSTRAINT transactionfeerecord_payment_item_id_fkey
        FOREIGN KEY (payment_item_id) REFERENCES paymentitem(id) ON DELETE CASCADE;

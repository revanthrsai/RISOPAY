CREATE TABLE payment_batch (
    id UUID PRIMARY KEY,
    instruction_id UUID NOT NULL,
    batch_id VARCHAR(15) NOT NULL UNIQUE,
    payment_info_id VARCHAR(35) NOT NULL,
    transaction_count INTEGER NOT NULL,
    control_sum NUMERIC(35, 17) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_payment_batch_instruction
        FOREIGN KEY (instruction_id) REFERENCES instruction (id),
    CONSTRAINT uq_payment_batch_instruction_payment_info
        UNIQUE (instruction_id, payment_info_id),
    CONSTRAINT uq_payment_batch_id_instruction
        UNIQUE (id, instruction_id),
    CONSTRAINT ck_payment_batch_transaction_count
        CHECK (transaction_count > 0),
    CONSTRAINT ck_payment_batch_control_sum
        CHECK (control_sum >= 0)
);

CREATE TABLE payment_transaction (
    id UUID PRIMARY KEY,
    instruction_id UUID NOT NULL,
    payment_batch_id UUID NOT NULL,
    transaction_id VARCHAR(15) NOT NULL UNIQUE,
    end_to_end_id VARCHAR(35) NOT NULL,
    instructed_amount NUMERIC(18, 5) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(32) NOT NULL,
    workflow_code VARCHAR(64) NOT NULL,
    workflow_state VARCHAR(16) NOT NULL,
    current_business_function VARCHAR(64) NOT NULL,
    correlation_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_payment_transaction_instruction
        FOREIGN KEY (instruction_id) REFERENCES instruction (id),
    CONSTRAINT fk_payment_transaction_batch_instruction
        FOREIGN KEY (payment_batch_id, instruction_id)
        REFERENCES payment_batch (id, instruction_id),
    CONSTRAINT ck_payment_transaction_currency
        CHECK (CHAR_LENGTH(currency) = 3),
    CONSTRAINT ck_payment_transaction_status
        CHECK (status IN (
            'PROCESSING', 'READY_TO_BULK', 'REJECTED',
            'REPAIR_REQUIRED', 'ACCEPTED'
        )),
    CONSTRAINT ck_payment_transaction_workflow_state
        CHECK (workflow_state IN (
            'PENDING', 'RUNNING', 'WAITING', 'COMPLETED', 'FAILED'
        )),
    CONSTRAINT ck_payment_transaction_amount
        CHECK (instructed_amount >= 0)
);

CREATE INDEX idx_payment_batch_instruction_id
    ON payment_batch (instruction_id);

CREATE INDEX idx_payment_transaction_instruction_id
    ON payment_transaction (instruction_id);

CREATE INDEX idx_payment_transaction_batch_id
    ON payment_transaction (payment_batch_id);

CREATE INDEX idx_payment_transaction_status
    ON payment_transaction (status, current_business_function);

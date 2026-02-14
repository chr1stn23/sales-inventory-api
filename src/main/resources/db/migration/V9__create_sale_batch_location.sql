CREATE TABLE sale_batch_allocations
(
    id               BIGSERIAL PRIMARY KEY,
    sale_detail_id   BIGINT  NOT NULL,
    product_batch_id BIGINT  NOT NULL,
    quantity         INTEGER NOT NULL CHECK (quantity > 0),

    CONSTRAINT fk_sale_batch_allocations_sale_detail
        FOREIGN KEY (sale_detail_id)
            REFERENCES sale_details (id),

    CONSTRAINT fk_sale_batch_allocations_product_batch
        FOREIGN KEY (product_batch_id)
            REFERENCES product_batches (id),

    CONSTRAINT uk_sale_detail_batch
        UNIQUE (sale_detail_id, product_batch_id)
)
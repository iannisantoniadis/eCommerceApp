create table if not exists category(
    id bigint generated always as identity PRIMARY KEY,
    description varchar(255),
    name varchar(255)
);

create table if not exists product(
    id bigint generated always as identity PRIMARY KEY,
    description varchar(255),
    name varchar(255),
    available_quantity double precision not null,
    price decimal(38, 2),
    category_id bigint,
    CONSTRAINT FK_product_cat_id_category_id
    FOREIGN KEY (category_id)
    REFERENCES category(id)
);
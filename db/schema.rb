# This file is auto-generated from the current state of the database. Instead
# of editing this file, please use the migrations feature of Active Record to
# incrementally modify your database, and then regenerate this schema definition.
#
# Note that this schema.rb definition is the authoritative source for your
# database schema. If you need to create the application database on another
# system, you should be using db:schema:load, not running all the migrations
# from scratch. The latter is a flawed and unsustainable approach (the more migrations
# you'll amass, the slower it'll run and the greater likelihood for issues).
#
# It's strongly recommended that you check this file into your version control system.

ActiveRecord::Schema.define(version: 20170819053158) do

  create_table "access_tokens", force: :cascade, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4" do |t|
    t.integer  "user_id"
    t.string   "token",                   collation: "latin1_swedish_ci"
    t.datetime "created_at", null: false
    t.datetime "updated_at", null: false
    t.index ["user_id"], name: "index_access_tokens_on_user_id", using: :btree
  end

  create_table "organization_ranks", force: :cascade, options: "ENGINE=InnoDB DEFAULT CHARSET=latin1" do |t|
    t.integer  "stargazers_count", null: false
    t.integer  "rank",             null: false
    t.datetime "created_at",       null: false
    t.datetime "updated_at",       null: false
    t.index ["rank"], name: "index_organization_ranks_on_rank", unique: true, using: :btree
    t.index ["stargazers_count"], name: "index_organization_ranks_on_stargazers_count", unique: true, using: :btree
  end

  create_table "repositories", force: :cascade, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4" do |t|
    t.string   "name"
    t.string   "full_name"
    t.integer  "owner_id"
    t.text     "description",      limit: 16777215
    t.boolean  "fork"
    t.datetime "created_at",                                    null: false
    t.datetime "updated_at",                                    null: false
    t.string   "homepage"
    t.integer  "stargazers_count",                  default: 0, null: false
    t.string   "language"
    t.datetime "fetched_at"
    t.index ["full_name"], name: "index_repositories_on_full_name", length: { full_name: 191 }, using: :btree
    t.index ["owner_id", "stargazers_count"], name: "index_repositories_on_owner_id_and_stargazers_count", using: :btree
    t.index ["stargazers_count"], name: "index_repositories_on_stargazers_count", using: :btree
  end

  create_table "repository_ranks", force: :cascade, options: "ENGINE=InnoDB DEFAULT CHARSET=latin1" do |t|
    t.integer  "stargazers_count", null: false
    t.integer  "rank",             null: false
    t.datetime "created_at",       null: false
    t.datetime "updated_at",       null: false
    t.index ["rank"], name: "index_repository_ranks_on_rank", unique: true, using: :btree
    t.index ["stargazers_count"], name: "index_repository_ranks_on_stargazers_count", unique: true, using: :btree
  end

  create_table "update_user_jobs", force: :cascade, options: "ENGINE=InnoDB DEFAULT CHARSET=latin1" do |t|
    t.string   "payload",                null: false
    t.datetime "timeout_at",             null: false
    t.bigint   "owner",      default: 0, null: false, unsigned: true
    t.datetime "created_at",             null: false
    t.datetime "updated_at",             null: false
    t.index ["timeout_at"], name: "index_update_user_jobs_on_timeout_at", using: :btree
  end

  create_table "user_ranks", force: :cascade, options: "ENGINE=InnoDB DEFAULT CHARSET=latin1" do |t|
    t.integer  "stargazers_count", null: false
    t.integer  "rank",             null: false
    t.datetime "created_at",       null: false
    t.datetime "updated_at",       null: false
    t.index ["rank"], name: "index_user_ranks_on_rank", unique: true, using: :btree
    t.index ["stargazers_count"], name: "index_user_ranks_on_stargazers_count", unique: true, using: :btree
  end

  create_table "users", force: :cascade, options: "ENGINE=InnoDB DEFAULT CHARSET=utf8mb4" do |t|
    t.string   "login",                                     collation: "utf8_unicode_ci"
    t.string   "avatar_url",                                collation: "utf8_unicode_ci"
    t.string   "type",                                      collation: "utf8_unicode_ci"
    t.datetime "created_at",                   null: false
    t.datetime "updated_at",                   null: false
    t.datetime "queued_at"
    t.integer  "stargazers_count", default: 0, null: false
    t.string   "location",                                  collation: "utf8_unicode_ci"
    t.index ["login"], name: "index_users_on_login", using: :btree
    t.index ["stargazers_count"], name: "index_users_on_stargazers_count", using: :btree
  end

end

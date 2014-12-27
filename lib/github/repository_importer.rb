module Github
  class RepositoryImporter
    API_REQUEST_INTERVAL = 1
    MAX_IMPORT_COUNT = 10
    FETCH_ATTRIBUTES = %i[
      id
      name
      full_name
      description
      fork
    ]

    def import
      logger.info 'Repository import task invoked'

      client = LimitBalancer.instance.client
      count  = Repository.last.id

      MAX_IMPORT_COUNT.times do
        import_repos(client)
        sleep API_REQUEST_INTERVAL
      end

      logger.info "Repository last #{count} => #{Repository.last.id}"
    rescue => e
      logger.error "#{e.class}: #{e}"
    end

    private

    def import_repos(client)
      last_id = Repository.pluck('MAX(id)').first
      repos   = []

      client.auto_paginate = false
      client.all_repositories(since: last_id).each do |row|
        repo = Repository.new
        repo.attributes = row.to_hash.slice(*FETCH_ATTRIBUTES)
        repo.owner_id   = row[:owner] && row[:owner][:id]
        repos << repo
      end

      Repository.import(repos)
    end

    def logger
      @logger ||= Logger.new('log/repository_importer.log')
    end
  end
end

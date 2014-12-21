module Github
  class Importer
    def import
      tokens = Rails.application.secrets[:github_access_tokens]
      tokens
    end
  end
end

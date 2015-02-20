package db

func AllTokens() []string {
	var token string
	tokens := []string{}

	rows, err := Query("SELECT token FROM access_tokens LIMIT 200;")
	if err != nil {
		return []string{}
	}
	defer rows.Close()

	for rows.Next() {
		rows.Scan(&token)
		tokens = append(tokens, token)
	}
	return tokens
}

func DeleteToken(token string) {
	Exec("DELETE FROM access_tokens WHERE token = ?;", token)
}

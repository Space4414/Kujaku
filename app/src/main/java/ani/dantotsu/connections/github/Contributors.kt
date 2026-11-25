package ani.dantotsu.connections.github

import ani.dantotsu.settings.Developer

class Contributors {

    fun getContributors(): Array<Developer> = arrayOf(
        Developer(
            name = "Space4414",
            pfp = "https://github.com/Space4414.png",
            role = "Lead Developer",
            url = "https://github.com/Space4414"
        )
    )
}
